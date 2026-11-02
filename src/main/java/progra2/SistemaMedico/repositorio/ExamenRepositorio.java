package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.Examen;

import java.util.List;

public interface ExamenRepositorio extends JpaRepository<Examen, Long> {

    List<Examen> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "laboratorio")
    Page<Examen> findByNombreContainingIgnoreCase(String texto, Pageable pageable);

    long countByLaboratorioIdAndActivoTrue(Long laboratorioId);
}
