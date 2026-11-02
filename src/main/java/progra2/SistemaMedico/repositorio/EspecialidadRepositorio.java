package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Especialidad;

import java.util.List;

public interface EspecialidadRepositorio extends JpaRepository<Especialidad, Long> {
    List<Especialidad> findByActivoTrue();

    Page<Especialidad> findByNombreContainingIgnoreCase(String texto, Pageable pageable);
}
