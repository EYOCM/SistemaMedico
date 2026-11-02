package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import progra2.SistemaMedico.model.Auditoria;

import java.util.List;

@Repository
public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String entidad, Long entidadId);

    List<Auditoria> findTop50ByOrderByFechaHoraDesc();
}
