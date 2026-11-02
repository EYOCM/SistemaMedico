package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Medicamento;

import java.util.List;

public interface MedicamentoRepositorio extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByActivoTrueOrderByNombreAsc();

    Page<Medicamento> findByNombreContainingIgnoreCase(String texto, Pageable pageable);
}
