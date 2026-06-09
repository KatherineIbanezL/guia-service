package cl.duoc.guia_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Subir archivos organizados correctamente en carpetas (fecha/entidad)
    public String subirArchivo(String entidad, File archivo) {
        String fechaCarpeta = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String s3Key = fechaCarpeta + "/" + entidad + "/" + archivo.getName();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
        return s3Key;
    }

    // Modificar y actualizar los archivos en AWS S3 (Sobrescribe el Key existente)
    public void actualizarArchivo(String s3Key, File nuevoArchivo) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(nuevoArchivo));
    }

    // Descargar los archivos desde AWS S3 asegurando integridad
    public byte[] descargarArchivo(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
                
        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }
}