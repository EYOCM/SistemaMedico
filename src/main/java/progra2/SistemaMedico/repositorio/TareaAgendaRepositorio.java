package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.TareaAgenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TareaAgendaRepositorio extends JpaRepository<TareaAgenda, Long> {

    List<TareaAgenda> findByMedicoIdAndFechaLimite(Long medicoId, LocalDate fecha);

    Optional<TareaAgenda> findByIdAndMedicoId(Long id, Long medicoId);
}
