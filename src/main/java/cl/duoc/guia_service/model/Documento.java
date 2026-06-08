package cl.duoc.guia_service.model;

import javax.persistence.*; 
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "documentos_cloud")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String tipoDocumento;
    private String transportistaEntity; 
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private String rutaEfs; 
    private String s3Key;   
    private String estado;  
}
