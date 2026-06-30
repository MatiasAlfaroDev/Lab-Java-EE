package com.chat.datatype;

public class UploadAdjuntoResponse {

    private String urlArchivo;
    private String nombreArchivo;
    private Long tamanoArchivo;
    private String mimeType;

    public UploadAdjuntoResponse() {
    }

    public UploadAdjuntoResponse(String urlArchivo, String nombreArchivo, Long tamanoArchivo, String mimeType) {
        this.urlArchivo = urlArchivo;
        this.nombreArchivo = nombreArchivo;
        this.tamanoArchivo = tamanoArchivo;
        this.mimeType = mimeType;
        
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public Long getTamanoArchivo() {
        return tamanoArchivo;
    }

    public void setTamanoArchivo(Long tamanoArchivo) {
        this.tamanoArchivo = tamanoArchivo;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}