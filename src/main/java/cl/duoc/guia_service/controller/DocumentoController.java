package cl.duoc.guia_service.controller;

import cl.duoc.guia_service.model.Guia;
import cl.duoc.guia_service.service.GuiaService;
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
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;
    private final S3Service s3Service;

    public GuiaController(GuiaService guiaService, S3Service s3Service) {
        this.guiaService = guiaService;
        this.s3Service = s3Service;
    }

    // 1. Crear guías de despacho (e integración automática de subida a S3)
    @PostMapping("/crear")
    public ResponseEntity<Guia> crearGuia(@RequestParam Long pedidoId, @RequestParam String transportista) {
        try {
            // Mock PDF binario simulando la estructura del documento de transporte
            byte[] mockPdfBytes = "%PDF-1.4 Mock Document Content - Guia Despacho".getBytes();
            Guia nuevaGuia = guiaService.procesarYCrearGuia(pedidoId, transportista, mockPdfBytes);
            return new ResponseEntity<>(nuevaGuia, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 2. Descargar guías con validación básica de permisos
    @GetMapping("/descargar/{id}")
    public ResponseEntity<byte[]> descargarGuia(@PathVariable Long id, @RequestHeader("X-Auth-Token") String token) {
        // Simulación de validación de permisos requerida
        if (token == null || !token.equals("TOKEN_VALIDO_DUOC")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Guia guia = guiaService.obtenerPorId(id);
            byte[] archivoS3 = s3Service.descargarDesdeS3(guia.getUrlS3());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "guia_" + id + ".pdf");
            
            return new ResponseEntity<>(archivoS3, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 3. Modificar o actualizar guías
    @PutMapping("/{id}")
    public ResponseEntity<Guia> actualizarGuia(@PathVariable Long id, @RequestBody Guia datosActualizados) {
        try {
            Guia modificada = guiaService.actualizarGuia(id, datosActualizados);
            return ResponseEntity.ok(modificada);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 4. Eliminar guías específicas
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarGuia(@PathVariable Long id) {
        try {
            guiaService.eliminarGuia(id);
            return ResponseEntity.ok("Guía eliminada exitosamente del ecosistema.");
        } catch (Exception e) {
            return new ResponseEntity<>("Error al procesar la eliminación: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 5. Consultar guías por transportista y rango de fechas
    @GetMapping("/consultar")
    public ResponseEntity<List<Guia>> consultarGuias(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        
        List<Guia> resultado = guiaService.buscarPorFiltros(transportista, inicio, fin);
        return ResponseEntity.ok(resultado);
    }
}