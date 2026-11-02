package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import progra2.SistemaMedico.model.EventoAgenda;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventoAgendaRepositorio extends JpaRepository<EventoAgenda, Long> {

    List<EventoAgenda> findByMedicoIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(
            Long medicoId, LocalDateTime hasta, LocalDateTime desde);

    Optional<EventoAgenda> findByIdAndMedicoId(Long id, Long medicoId);
}
