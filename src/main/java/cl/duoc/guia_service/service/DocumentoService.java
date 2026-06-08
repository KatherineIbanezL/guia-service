package cl.duoc.guia_service.service;

import cl.duoc.guia_service.model.Documento;
import cl.duoc.guia_service.repository.DocumentoRepository; // Asegúrate de renombrar la interfaz del repositorio
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final EFSService efsService;
    private final S3Service s3Service;

    public DocumentoService(DocumentoRepository documentoRepository, EFSService efsService, S3Service s3Service) {
        this.documentoRepository = documentoRepository;
        this.efsService = efsService;
        this.s3Service = s3Service;
    }

    // Flujo inicial de carga (Criterio 1 y Criterio 2)
    public Documento registrarDocumento(String transportista, String nombre, byte[] contenido) throws Exception {
        Documento doc = new Documento();
        doc.setNombreArchivo(nombre);
        doc.setTipoDocumento("GUIA_DESPACHO");
        doc.setTransportistaEntity(transportista);
        doc.setFechaCreacion(LocalDateTime.now());
        doc.setEstado("ACTIVO");
        doc = documentoRepository.save(doc);

        // Almacenamiento temporal en EFS (Criterio 1)
        String rutaEfs = efsService.guardarTemporalmente(doc.getId(), contenido);
        doc.setRutaEfs(rutaEfs);

        // Subida automática a S3 (Criterio 2)
        File archivoTemp = new File(rutaEfs);
        String s3Key = s3Service.subirArchivo(transportista, archivoTemp);
        doc.setS3Key(s3Key);

        return documentoRepository.save(doc);
    }

    // Criterio 3: Modificar y actualizar archivos en S3 y BD
    public Documento actualizarDocumento(Long id, byte[] nuevoContenido) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        // 1. Actualizar el archivo físico temporal en EFS
        efsService.guardarTemporalmente(doc.getId(), nuevoContenido);
        
        // 2. Modificar el archivo directo en S3 usando la misma Key de origen
        File archivoActualizado = new File(doc.getRutaEfs());
        s3Service.actualizarArchivo(doc.getS3Key(), archivoActualizado);

        // 3. Actualizar metadatos de auditoría en Oracle Cloud
        doc.setFechaModificacion(LocalDateTime.now());
        doc.setEstado("MODIFICADO");
        
        return documentoRepository.save(doc);
    }

    // Criterio 4: eliminar guías específicas
    public void eliminarDocumentoFisicoYLogico(Documento doc) throws Exception {
    // 1. Eliminar del sistema de archivos temporal EFS
    efsService.eliminarDeEfs(doc.getRutaEfs());
    
    // 2. Eliminar el registro en la base de datos de Oracle Cloud
    documentoRepository.delete(doc);
    }   

    // Criterio 5: Consultar el historial de archivos por entidad y rango de fechas
    public List<Documento> consultarHistorial(String entidad, LocalDateTime inicio, LocalDateTime fin) {
        return documentoRepository.findByTransportistaEntityAndFechaCreacionBetween(entidad, inicio, fin);
    }

    public Documento obtenerPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ID no encontrada"));
    }
}