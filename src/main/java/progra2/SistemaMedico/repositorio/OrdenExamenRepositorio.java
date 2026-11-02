package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.OrdenExamen;

public interface OrdenExamenRepositorio extends JpaRepository<OrdenExamen, Long> {
}
