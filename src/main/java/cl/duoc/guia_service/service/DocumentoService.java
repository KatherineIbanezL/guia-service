package cl.duoc.guia_service.service;

import cl.duoc.guia_service.model.Documento;
import cl.duoc.guia_service.repository.DocumentoRepository; 
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

    // 1. REGISTRAR / SUBIR
    public Documento registrarDocumento(String transportista, String nombre, byte[] mockPdf) throws Exception {
        Documento doc = new Documento();
        doc.setNombreArchivo(nombre);
        doc.setTipoDocumento("GUIA_DESPACHO");
        doc.setTransportistaEntity(transportista);
        doc.setFechaCreacion(LocalDateTime.now());
        doc.setEstado("ACTIVO");
        
        // Guardar en Oracle para obtener el ID secuencial autoincrementable
        doc = documentoRepository.saveAndFlush(doc);

        // Almacenamiento temporal en EFS 
        String rutaEfs = efsService.guardarTemporalmente(doc.getId(), mockPdf);
        doc.setRutaEfs(rutaEfs);

        // Construir Key estructurada por Carpeta con el Número de Resumen (ID)
        String s3KeyEstructurada = "resumenes/" + doc.getId() + "/" + nombre;
        
        File archivoTemp = new File(rutaEfs);
        // Envia la Key estructurada directamente al S3Service
        String s3Key = s3Service.subirArchivoConKey(s3KeyEstructurada, archivoTemp);
        doc.setS3Key(s3Key);

        return documentoRepository.saveAndFlush(doc);
    }

    // 2. MODIFICAR / ACTUALIZAR
    public Documento actualizarDocumento(Long id, byte[] nuevoContenido) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resumen de inscripción no encontrado con el ID: " + id));

        // 1. Actualizar el archivo físico temporal en EFS
        efsService.guardarTemporalmente(doc.getId(), nuevoContenido);
        
        // 2. Modificar el archivo directo en S3 usando la Key estructurada existente
        File archivoActualizado = new File(doc.getRutaEfs());
        s3Service.actualizarArchivo(doc.getS3Key(), archivoActualizado);

        // 3. Actualizar metadatos de auditoría en Oracle Cloud
        doc.setFechaModificacion(LocalDateTime.now());
        doc.setEstado("MODIFICADO");
        
        return documentoRepository.saveAndFlush(doc);
    }

    // 3. DESCARGAR
    public byte[] descargarArchivoS3(Long id) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento para descargar con el ID: " + id));
        
        if (doc.getS3Key() == null || doc.getS3Key().isEmpty()) {
            throw new IllegalStateException("El documento no posee una llave válida de AWS S3 asociada.");
        }
        
        // Descarga el arreglo de bytes puro desde el bucket de AWS S3
        return s3Service.descargarArchivo(doc.getS3Key());
    }

    // 4. BORRAR / ELIMINAR
    public void eliminarDocumentoFisicoYLogico(Long id) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se puede eliminar un registro inexistente con ID: " + id));

        // 1. Eliminar del sistema de archivos temporal EFS
        if (doc.getRutaEfs() != null) {
            efsService.eliminarDeEfs(doc.getRutaEfs());
        }
        
        // 2. Eliminar el objeto almacenado en AWS S3
        if (doc.getS3Key() != null && !doc.getS3Key().isEmpty()) {
            try {
                s3Service.eliminarArchivo(doc.getS3Key());
            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                System.err.println("Advertencia de AWS: Falló remoción física en S3: " + e.awsErrorDetails().errorMessage());
            }
        }

        // 3. Eliminar el registro en la base de datos de Oracle Cloud
        documentoRepository.delete(doc);
    } 

    // 5. CONNSULTAR HISTORIAL
    public List<Documento> consultarHistorial(String transportista, LocalDateTime inicio, LocalDateTime fin) {
        return documentoRepository.findByTransportistaEntityAndFechaCreacionBetween(transportista, inicio, fin);
    }

    public Documento obtenerPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de documento no encontrada: " + id));
    }

}