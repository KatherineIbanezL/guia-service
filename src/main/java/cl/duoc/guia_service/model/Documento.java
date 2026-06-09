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
    @Column(name = "id", insertable = false, updatable = false)
    private Long id;

    private String nombreArchivo;
    private String tipoDocumento;
    private String transportistaEntity; 
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private String rutaEfs; 
    private String s3Key;   
    private String estado;
    
   
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getTransportistaEntity() { return transportistaEntity; }
    public void setTransportistaEntity(String transportistaEntity) { this.transportistaEntity = transportistaEntity; }

    public java.time.LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(java.time.LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public java.time.LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(java.time.LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public String getRutaEfs() { return rutaEfs; }
    public void setRutaEfs(String rutaEfs) { this.rutaEfs = rutaEfs; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
