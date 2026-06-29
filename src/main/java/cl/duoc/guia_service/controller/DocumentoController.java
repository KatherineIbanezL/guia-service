package cl.duoc.guia_service.controller;

import cl.duoc.guia_service.model.Documento;
import cl.duoc.guia_service.service.DocumentoService;
import cl.duoc.guia_service.service.S3Service;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final S3Service s3Service;

    public DocumentoController(DocumentoService documentoService, S3Service s3Service) {
        this.documentoService = documentoService;
        this.s3Service = s3Service;
    }

    // Crear almacenamiento EFS y subir automático a S3
    @PostMapping("/generar")
    public ResponseEntity<?> generarDocumento(
            @RequestParam String transportista,
            @RequestParam String nombreArchivo) {
        try {
            byte[] mockPdf = "%PDF-1.4 Eandar de Guia de Despacho Logistica".getBytes();
            Documento nuevoDoc = documentoService.registrarDocumento(transportista, nombreArchivo, mockPdf);
            return new ResponseEntity<>(nuevoDoc, HttpStatus.CREATED);
            
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // Error específico 1: Fallo en AWS S3 (Credenciales, tokens, permisos del bucket)
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error en servicio AWS S3: " + e.awsErrorDetails().errorMessage());
                    
        } catch (java.io.IOException e) {
            // Error específico 2: Fallo físico de escritura en EFS / Carpeta local
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error de infraestructura local: No se pudo escribir en el almacenamiento temporal EFS.");
                    
        } catch (Exception e) {
            // Error genérico de respaldo: Por si falla la Base de Datos Oracle u otra cosa
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno no controlado en el servidor: " + e.getMessage());
        }
    }

    // Modificar y actualizar los archivos en S3
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarDocumento(
            @PathVariable Long id,
            @RequestBody String nuevoContenidoTexto) {
        try {
            byte[] nuevosBytes = nuevoContenidoTexto.getBytes();
            Documento docActualizado = documentoService.actualizarDocumento(id, nuevosBytes);
            return ResponseEntity.ok(docActualizado);
            
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // 1. ESPECÍFICO DE S3
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error al actualizar objeto en AWS S3: " + e.awsErrorDetails().errorMessage());
                    
        } catch (RuntimeException e) {
            // 2. GENERAL DE RUNTIME
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error de negocio: " + e.getMessage());
                    
        } catch (Exception e) {
            // 3. RESPALDO GENÉRICO
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno inesperado al actualizar: " + e.getMessage());
        }
    }

    // Descargar los archivos desde AWS S3
    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargarDocumento(@PathVariable Long id) {
        try {
            Documento doc = documentoService.obtenerPorId(id);
            byte[] archivoBytes = s3Service.descargarArchivo(doc.getS3Key());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", doc.getNombreArchivo());

            return new ResponseEntity<>(archivoBytes, headers, HttpStatus.OK);

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // El archivo no existe en el Bucket o hay problemas de credenciales AWS
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error en el almacenamiento AWS S3: " + e.awsErrorDetails().errorMessage());

        } catch (RuntimeException e) {
            // No existe el ID en la base de datos Oracle
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error de consulta: El ID " + id + " no existe en los registros.");
                    
                    
        } catch (Exception e) {
            // Error genérico de respaldo
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado al intentar descargar el archivo: " + e.getMessage());
        }
    }

    // Endpoint obligatorio
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarDocumento(@PathVariable Long id) {
        try {
            // Buscamos el documento en Oracle Cloud
            Documento doc = documentoService.obtenerPorId(id);
            
            documentoService.eliminarDocumentoFisicoYLogico(doc);
            return ResponseEntity.ok("Archivo y registros eliminados con éxito.");

        } catch (RuntimeException e) {
            // Error específico 1: El documento a eliminar no existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error de eliminación: No se puede borrar porque el documento no existe.");
                    
        } catch (Exception e) {
            // Captura fallos al intentar borrar físicamente en S3, EFS o DB
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    // Consulta el historial de archivos generados (con filtros)
    @GetMapping("/historial")
    public ResponseEntity<?> consultarHistorial(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        try {
            List<Documento> historial = documentoService.consultarHistorial(transportista, inicio, fin);
            
            if (historial.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // O un mensaje de "No hay registros"
            }
            return ResponseEntity.ok(historial);

        } catch (Exception e) {
            // Captura fallos de conectividad con el pool de base de datos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al consultar el historial en la base de datos: " + e.getMessage());
        }
    }

}