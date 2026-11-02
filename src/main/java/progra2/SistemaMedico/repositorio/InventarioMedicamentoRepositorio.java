package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.InventarioMedicamento;

import java.util.Optional;

public interface InventarioMedicamentoRepositorio extends JpaRepository<InventarioMedicamento, Long> {

    Optional<InventarioMedicamento> findByMedicamentoIdAndSucursalIdAndActivoTrue(Long medicamentoId, Long sucursalId);

    Optional<InventarioMedicamento> findByMedicamentoIdAndSucursalId(Long medicamentoId, Long sucursalId);

    @EntityGraph(attributePaths = {"medicamento", "sucursal"})
    Optional<InventarioMedicamento> findOneById(Long id);

    @EntityGraph(attributePaths = {"medicamento", "sucursal"})
    Page<InventarioMedicamento> findByMedicamentoNombreContainingIgnoreCaseAndActivoTrue(String texto, Pageable pageable);

    @EntityGraph(attributePaths = {"medicamento", "sucursal"})
    Page<InventarioMedicamento> findByMedicamentoNombreContainingIgnoreCaseAndSucursalIdAndActivoTrue(
            String texto, Long sucursalId, Pageable pageable);
}
