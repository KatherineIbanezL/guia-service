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
    public ResponseEntity<Documento> generarDocumento(
            @RequestParam String transportista,
            @RequestParam String nombreArchivo) {
        try {
            byte[] mockPdf = "%PDF-1.4 Estandar de Guia de Despacho Logistica".getBytes();
            Documento nuevoDoc = documentoService.registrarDocumento(transportista, nombreArchivo, mockPdf);
            return new ResponseEntity<>(nuevoDoc, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Modificar y actualizar los archivos en S3
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<Documento> actualizarDocumento(
            @PathVariable Long id,
            @RequestBody String nuevoContenidoTexto) {
        try {
            byte[] nuevosBytes = nuevoContenidoTexto.getBytes();
            Documento docActualizado = documentoService.actualizarDocumento(id, nuevosBytes);
            return ResponseEntity.ok(docActualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Descargar los archivos desde AWS S3
    @GetMapping("/descargar/{id}")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable Long id) {
        try {
            Documento doc = documentoService.obtenerPorId(id);
            byte[] archivoBytes = s3Service.descargarArchivo(doc.getS3Key());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", doc.getNombreArchivo());

            return new ResponseEntity<>(archivoBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    // Consulta el historial de archivos generados (con filtros)
    @GetMapping("/historial")
    public ResponseEntity<List<Documento>> consultarHistorial(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        
        List<Documento> historial = documentoService.consultarHistorial(transportista, inicio, fin);
        return ResponseEntity.ok(historial);
    }

}