/* Módulo de cripto E2E — ECDH P-256 + AES-GCM, WebCrypto nativo */
/* ponytail: sin dependencias externas, todo WebCrypto */
const Crypto = (() => {
  "use strict";

  const DB_NAME = "e2e-keys";
  const DB_VERSION = 1;
  const STORE = "keys";

  // IndexedDB es por origen, no por cuenta: sin namespacear por usuario, el segundo
  // usuario que loguea en el mismo navegador encontraba la clave del primero cacheada
  // y nunca generaba/subía la suya ("ya existe"), dejando al destinatario real sin
  // clave pública registrada en el server.
  let currentUserId = null;
  const keyName = () => currentUserId != null ? `ecdhPriv:${currentUserId}` : "ecdhPriv";
  // La privada se guarda como CryptoKey no exportable: no hay forma de re-derivar su
  // pública después. Se cachea la pública aparte para poder re-subirla en logins
  // futuros (p. ej. si cambia el formato de export, como con el fix de raw vs spki).
  const pubKeyName = () => currentUserId != null ? `ecdhPub:${currentUserId}` : "ecdhPub";

  // ── IndexedDB helpers ──────────────────────────────────────────────
  function openDB() {
    return new Promise((res, rej) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = (ev) => {
        ev.target.result.createObjectStore(STORE, { keyPath: "name" });
      };
      req.onsuccess = (ev) => res(ev.target.result);
      req.onerror = () => rej(req.error);
    });
  }

  async function dbGet(name) {
    const db = await openDB();
    return new Promise((res, rej) => {
      const req = db.transaction(STORE, "readonly").objectStore(STORE).get(name);
      req.onsuccess = () => res(req.result?.value ?? null);
      req.onerror = () => rej(req.error);
    });
  }

  async function dbPut(name, value) {
    const db = await openDB();
    return new Promise((res, rej) => {
      const req = db.transaction(STORE, "readwrite").objectStore(STORE).put({ name, value });
      req.onsuccess = () => res();
      req.onerror = () => rej(req.error);
    });
  }

  // ── Encoding helpers ───────────────────────────────────────────────
  const enc = new TextEncoder();
  const dec = new TextDecoder();

  function b64ToBytes(b64) {
    return Uint8Array.from(atob(b64), c => c.charCodeAt(0));
  }

  function bytesToB64(buf) {
    return btoa(String.fromCharCode(...new Uint8Array(buf)));
  }

  /** Empaca iv (12 bytes) + ct en base64 */
  function pack(iv, ct) {
    const combined = new Uint8Array(iv.length + ct.byteLength);
    combined.set(iv, 0);
    combined.set(new Uint8Array(ct), iv.length);
    return bytesToB64(combined);
  }

  /** Desempaca base64 → { iv: Uint8Array(12), ct: ArrayBuffer } */
  function unpack(b64) {
    const buf = b64ToBytes(b64);
    return { iv: buf.slice(0, 12), ct: buf.slice(12).buffer };
  }

  // ── Importar clave pública (punto EC crudo sin comprimir, 65 bytes) ────
  // El cliente Expo (noble/@noble/curves) no habla SPKI/DER: trabaja con el punto
  // crudo. Usar "raw" en vez de "spki" es lo que ambos clientes pueden producir e
  // importar sin librerías extra — con "spki" la pública de Expo (33 o 65 bytes,
  // sin envoltorio ASN.1) hacía tirar DataError acá antes de llegar siquiera a
  // derivar la clave AES.
  async function importPub(b64) {
    return crypto.subtle.importKey(
      "raw", b64ToBytes(b64),
      { name: "ECDH", namedCurve: "P-256" },
      true, []
    );
  }

  // ── Derivar clave AES de secreto ECDH ─────────────────────────────
  async function deriveAes(privKey, theirPub, usage) {
    return crypto.subtle.deriveKey(
      { name: "ECDH", public: theirPub },
      privKey,
      { name: "AES-GCM", length: 256 },
      false,
      [usage]
    );
  }

  // ══════════════════════════════════════════════════════════════════
  // API pública
  // ══════════════════════════════════════════════════════════════════

  /**
   * Genera el par ECDH si no existe, guarda la privada en IndexedDB
   * y sube la pública al server usando apiFn.
   * apiFn(method, path, body) — misma firma que api() en app.js.
   */
  async function ensureKeyPair(apiFn, userId) {
    currentUserId = userId;
    const existing = await dbGet(keyName());
    if (existing) {
      // Ya existe la privada: re-sube la pública cacheada por si el server la perdió
      // o el formato cambió. Si no hay pública cacheada (instalación de antes de este
      // fix, formato SPKI viejo), no hay forma de recuperarla de la privada no
      // exportable — queda como estaba hasta que se limpie el IndexedDB y regenere.
      const cachedPub = await dbGet(pubKeyName());
      if (cachedPub) await apiFn("PUT", "api/usuarios/clave-publica", { clavePub: cachedPub });
      return;
    }

    const pair = await crypto.subtle.generateKey(
      { name: "ECDH", namedCurve: "P-256" },
      false,           // privada no exportable
      ["deriveKey"]
    );
    await dbPut(keyName(), pair.privateKey);

    const pubRaw = await crypto.subtle.exportKey("raw", pair.publicKey);
    const pubB64 = bytesToB64(pubRaw);
    await dbPut(pubKeyName(), pubB64);
    await apiFn("PUT", "api/usuarios/clave-publica", { clavePub: pubB64 });
  }

  /** Devuelve la CryptoKey privada local del usuario actual, o null si no existe. */
  async function getPrivKey() {
    return dbGet(keyName());
  }

  // ── Cifrado 1:1 (ECDH directo) ─────────────────────────────────────

  async function encryptDirect(text, recipientPubB64) {
    const priv = await getPrivKey();
    const theirPub = await importPub(recipientPubB64);
    const aesKey = await deriveAes(priv, theirPub, "encrypt");
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, aesKey, enc.encode(text));
    return pack(iv, ct);
  }

  async function decryptDirect(payload, senderPubB64) {
    try {
      const priv = await getPrivKey();
      if (!priv) return null;
      const theirPub = await importPub(senderPubB64);
      const aesKey = await deriveAes(priv, theirPub, "decrypt");
      const { iv, ct } = unpack(payload);
      const plain = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, aesKey, ct);
      return dec.decode(plain);
    } catch { return null; }
  }

  // ── Claves de grupo ────────────────────────────────────────────────

  async function generateGroupKey() {
    return crypto.subtle.generateKey(
      { name: "AES-GCM", length: 256 }, true, ["encrypt", "decrypt"]
    );
  }

  async function exportGroupKey(ck) {
    const raw = await crypto.subtle.exportKey("raw", ck);
    return bytesToB64(raw);
  }

  async function importGroupKey(b64) {
    return crypto.subtle.importKey(
      "raw", b64ToBytes(b64),
      { name: "AES-GCM" }, true, ["encrypt", "decrypt"]
    );
  }

  /**
   * Envuelve (cifra) la clave de grupo para un miembro receptor.
   * Usa ECDH(miPriv, recipientPub) como clave de envoltura.
   */
  async function wrapGroupKey(groupKeyCk, recipientPubB64) {
    const priv = await getPrivKey();
    const theirPub = await importPub(recipientPubB64);
    const wrappingKey = await deriveAes(priv, theirPub, "encrypt");
    const rawGroup = await crypto.subtle.exportKey("raw", groupKeyCk);
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const wrapped = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, wrappingKey, rawGroup);
    return pack(iv, wrapped);
  }

  /**
   * Desenvuelve la clave de grupo.
   * distribuidorPubB64: quien distribuyó la clave (se usó su privada para envolver).
   */
  async function unwrapGroupKey(wrappedB64, distribuidorPubB64) {
    try {
      const priv = await getPrivKey();
      if (!priv) return null;
      const distribPub = await importPub(distribuidorPubB64);
      const unwrappingKey = await deriveAes(priv, distribPub, "decrypt");
      const { iv, ct } = unpack(wrappedB64);
      const rawGroup = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, unwrappingKey, ct);
      return importGroupKey(bytesToB64(rawGroup));
    } catch { return null; }
  }

  async function encryptGroup(text, groupKeyCk) {
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, groupKeyCk, enc.encode(text));
    return pack(iv, ct);
  }

  async function decryptGroup(payload, groupKeyCk) {
    try {
      if (!groupKeyCk) return null;
      const { iv, ct } = unpack(payload);
      const plain = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, groupKeyCk, ct);
      return dec.decode(plain);
    } catch { return null; }
  }

  // ── Adjuntos ───────────────────────────────────────────────────────

  /**
   * Cifra un Blob de archivo.
   * Devuelve { ciphertextB64, fileKeyB64 }.
   * El llamador incluye fileName y mimeType en el payload del mensaje cifrado.
   */
  async function encryptFile(blob) {
    const fileKey = await crypto.subtle.generateKey(
      { name: "AES-GCM", length: 256 }, true, ["encrypt"]
    );
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const buf = await blob.arrayBuffer();
    const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, fileKey, buf);
    const fileKeyB64 = await exportGroupKey(fileKey);
    return { ciphertextB64: pack(iv, ct), fileKeyB64 };
  }

  /**
   * Descifra un Blob cifrado. Devuelve un Blob con el contenido original,
   * o null si falla.
   */
  async function decryptFile(ciphertextB64, fileKeyB64) {
    try {
      const dk = await importGroupKey(fileKeyB64);
      const { iv, ct } = unpack(ciphertextB64);
      const plain = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, dk, ct);
      return new Blob([plain]);
    } catch { return null; }
  }

  // ── Tests ──────────────────────────────────────────────────────────

  /**
   * Ejecutar desde la consola del navegador: await Crypto.runTests()
   * Requiere que el usuario haya iniciado sesión (ensureKeyPair ya corrió).
   */
  async function runTests() {
    const pass = (name) => console.log(`%c✓ ${name}`, "color:green");
    const fail = (name, e) => console.error(`✗ ${name}`, e);

    // Test 1: round-trip 1:1 entre dos pares efímeros
    try {
      const a = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveKey"]);
      const b = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveKey"]);
      const aPubRaw = await crypto.subtle.exportKey("raw", a.publicKey);
      const bPubRaw = await crypto.subtle.exportKey("raw", b.publicKey);
      const aPubB64 = bytesToB64(aPubRaw);
      const bPubB64 = bytesToB64(bPubRaw);

      // Guardar temporalmente privadas para los helpers
      const origPriv = await dbGet("ecdhPriv");
      await dbPut("ecdhPriv", a.privateKey);

      const ct = await encryptDirect("hola E2E", bPubB64);
      await dbPut("ecdhPriv", b.privateKey);
      const plain = await decryptDirect(ct, aPubB64);
      if (plain !== "hola E2E") throw new Error("texto no coincide: " + plain);
      pass("1:1 encrypt/decrypt round-trip");

      // Test 2: clave equivocada falla
      const wrong = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveKey"]);
      const wrongPubRaw = await crypto.subtle.exportKey("raw", wrong.publicKey);
      await dbPut("ecdhPriv", b.privateKey);
      const res = await decryptDirect(ct, bytesToB64(wrongPubRaw));
      if (res !== null) throw new Error("debería devolver null con clave incorrecta");
      pass("1:1 decrypt con clave incorrecta → null");

      // Restaurar privada original
      if (origPriv) await dbPut("ecdhPriv", origPriv);
    } catch (e) { fail("1:1", e); }

    // Test 3: round-trip grupo
    try {
      const gk = await generateGroupKey();
      const ct = await encryptGroup("mensaje grupo", gk);
      const plain = await decryptGroup(ct, gk);
      if (plain !== "mensaje grupo") throw new Error("texto no coincide: " + plain);
      pass("Grupo encrypt/decrypt round-trip");
    } catch (e) { fail("Grupo", e); }

    // Test 4: wrap/unwrap clave de grupo entre dos pares efímeros
    try {
      const a = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveKey"]);
      const b = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveKey"]);
      const aPubRaw = await crypto.subtle.exportKey("raw", a.publicKey);
      const bPubRaw = await crypto.subtle.exportKey("raw", b.publicKey);

      const gk = await generateGroupKey();
      const origPriv = await dbGet("ecdhPriv");

      await dbPut("ecdhPriv", a.privateKey);
      const wrapped = await wrapGroupKey(gk, bytesToB64(bPubRaw));

      await dbPut("ecdhPriv", b.privateKey);
      const unwrapped = await unwrapGroupKey(wrapped, bytesToB64(aPubRaw));
      const ct = await encryptGroup("grupo wrap", gk);
      const plain = await decryptGroup(ct, unwrapped);
      if (plain !== "grupo wrap") throw new Error("texto no coincide: " + plain);
      pass("Grupo wrap/unwrap round-trip");

      if (origPriv) await dbPut("ecdhPriv", origPriv);
    } catch (e) { fail("wrap/unwrap", e); }

    // Test 5: adjunto round-trip
    try {
      const original = new Blob(["contenido de prueba"], { type: "text/plain" });
      const { ciphertextB64, fileKeyB64 } = await encryptFile(original);
      const decrypted = await decryptFile(ciphertextB64, fileKeyB64);
      const text = await decrypted.text();
      if (text !== "contenido de prueba") throw new Error("contenido no coincide: " + text);
      pass("Adjunto encrypt/decrypt round-trip");
    } catch (e) { fail("Adjunto", e); }

    console.log("Tests completos.");
  }

  return {
    ensureKeyPair,
    getPrivKey,
    encryptDirect,
    decryptDirect,
    generateGroupKey,
    exportGroupKey,
    importGroupKey,
    wrapGroupKey,
    unwrapGroupKey,
    encryptGroup,
    decryptGroup,
    encryptFile,
    decryptFile,
    runTests,
  };
})();
