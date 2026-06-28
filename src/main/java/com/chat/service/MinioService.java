package com.chat.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;

import java.io.InputStream;
import java.util.UUID;
import io.minio.GetObjectArgs;

@ApplicationScoped
public class MinioService {

    private final MinioClient client;
    private final String bucket;

    public MinioService() {

        String url = System.getenv("MINIO_URL");
        String accessKey = System.getenv("MINIO_ACCESS_KEY");
        String secretKey = System.getenv("MINIO_SECRET_KEY");
        bucket = System.getenv("MINIO_BUCKET");

        if (url == null || accessKey == null || secretKey == null || bucket == null) {
            throw new RuntimeException("Faltan variables de entorno de MinIO");
        }

        client = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();

        try {

            boolean existe = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build());

            if (!existe) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error inicializando MinIO", e);
        }
    }

    public String subirArchivo(
        InputStream inputStream,
        String nombreArchivo,
        long tamano,
        String tipoContenido) {

        try {

            // Generar un nombre único para evitar colisiones
            String nombreUnico = UUID.randomUUID() + "-" + nombreArchivo;

            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(nombreUnico)
                            .stream(inputStream, tamano, -1)
                            .contentType(tipoContenido)
                            .build()
            );

            // Devolver la ruta del objeto
            return nombreUnico;

        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO", e);
        }
    }

    public InputStream descargarArchivo(String nombreObjeto) {

        try {

            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(nombreObjeto)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error al descargar archivo de MinIO", e);
        }
    }

    public String obtenerMimeType(String nombreObjeto) {
        try {
            StatObjectResponse stat =
                client.statObject(
                    StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(nombreObjeto)
                        .build()
                );
            return stat.contentType();
        } catch (Exception e) {
            throw new RuntimeException(
                "Error obteniendo tipo de archivo",
                e
            );
        }
    }
}