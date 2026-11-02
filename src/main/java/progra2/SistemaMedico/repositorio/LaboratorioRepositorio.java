package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Laboratorio;

import java.util.List;

public interface LaboratorioRepositorio extends JpaRepository<Laboratorio, Long> {

    Page<Laboratorio> findByNombreContainingIgnoreCase(String texto, Pageable pageable);

    List<Laboratorio> findByActivoTrueOrderByNombreAsc();
}
