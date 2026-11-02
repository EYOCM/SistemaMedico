package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.SucursalEspecialidad;

import java.util.Optional;

public interface SucursalEspecialidadRepositorio extends JpaRepository<SucursalEspecialidad, Long> {

    @Override
    @EntityGraph(attributePaths = {"sucursal", "especialidad"})
    Page<SucursalEspecialidad> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"sucursal", "especialidad"})
    Page<SucursalEspecialidad> findByIdEquals(Long id, Pageable pageable);

    Optional<SucursalEspecialidad> findBySucursalIdAndEspecialidadId(Long sucursalId, Long especialidadId);
}
