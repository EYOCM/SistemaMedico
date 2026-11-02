package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.OrdenLaboratorio;
import progra2.SistemaMedico.model.enums.EstadoOrdenLaboratorio;

import java.util.List;
import java.util.Optional;

public interface OrdenLaboratorioRepositorio extends JpaRepository<OrdenLaboratorio, Long> {

    Optional<OrdenLaboratorio> findByCitaId(Long citaId);

    boolean existsByCitaId(Long citaId);
    Optional<OrdenLaboratorio> findByNumeroOrden(String numeroOrden);

    List<OrdenLaboratorio> findByCitaPacienteDpiAndEstadoAndExternaFalseOrderByFechaCreacionAsc(
            String dpi, EstadoOrdenLaboratorio estado);

    List<OrdenLaboratorio> findAllByOrderByFechaCreacionDesc();
}


