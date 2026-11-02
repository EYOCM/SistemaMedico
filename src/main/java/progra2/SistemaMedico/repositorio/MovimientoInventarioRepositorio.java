package progra2.SistemaMedico.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import progra2.SistemaMedico.model.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoInventarioRepositorio extends JpaRepository<MovimientoInventario, Long>,
        JpaSpecificationExecutor<MovimientoInventario> {

    @Override
    @EntityGraph(attributePaths = {"medicamento", "sucursal"})
    Page<MovimientoInventario> findAll(Specification<MovimientoInventario> filtro, Pageable pageable);

    @EntityGraph(attributePaths = {"medicamento", "sucursal"})
    Optional<MovimientoInventario> findOneById(Long id);

    @EntityGraph(attributePaths = {"medicamento", "sucursal"})
    List<MovimientoInventario> findTop10ByMedicamentoIdAndSucursalIdOrderByFechaDescIdDesc(Long medicamentoId,
                                                                                         Long sucursalId);

    @Query("""
            SELECT m.tipo, SUM(m.cantidad) FROM MovimientoInventario m
            WHERE m.medicamento.id = :medicamentoId AND m.sucursal.id = :sucursalId AND m.activo = true
              AND m.fecha >= :desde AND m.fecha < :hasta
            GROUP BY m.tipo
            """)
    List<Object[]> totalesPorTipo(@Param("medicamentoId") Long medicamentoId,
                                  @Param("sucursalId") Long sucursalId,
                                  @Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta);
}
