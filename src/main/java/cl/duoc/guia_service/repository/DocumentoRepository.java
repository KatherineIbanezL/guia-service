package cl.duoc.guia_service.repository;

import cl.duoc.guia_service.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByTransportistaEntityAndFechaCreacionBetween(String transportistaEntity, LocalDateTime inicio, LocalDateTime fin);
}