package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepositorio extends JpaRepository<Cita, Long> {

    // FA03 - CU-03 INICIO
    /**
     * 1. Las canceladas. Si un paciente cancela, su horario vuelve a estar libre.
     * 2. Las reservas vencidas (FA03). Una cita queda en PENDIENTE_PAGO con cinco
     * minutos para pagarse; si nadie paga, el horario se libera solo.
     *
     **/

    @Query("""

            SELECT c.fechaHora FROM Cita c
           WHERE c.medico.id = :medicoId
             AND DATE(c.fechaHora) = :fecha
             AND c.estado <> :cancelada
             AND ( c.estado <> :pendientePago
                   OR c.fechaReservaExpira IS NULL
                   OR c.fechaReservaExpira > :ahora )
           """)
    List<LocalDateTime> findHorariosOcupados(@Param("medicoId") Long medicoId,
                                             @Param("fecha") LocalDate fecha,
                                             @Param("ahora") LocalDateTime ahora,
                                             @Param("cancelada") AppointmentStatus cancelada,
                                             @Param("pendientePago") AppointmentStatus pendientePago);


    default List<LocalDateTime> findHorariosOcupados(Long medicoId, LocalDate fecha) {
        return findHorariosOcupados(medicoId, fecha, LocalDateTime.now(),
                AppointmentStatus.CANCELADA, AppointmentStatus.PENDIENTE_PAGO);
    }
    // FA03 - CU-03 FINAL

    List<Cita> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    Optional<Cita> findByNumeroCita(String numeroCita);

    List<Cita> findByPacienteIdAndEstadoInAndFechaHoraGreaterThanEqualOrderByFechaHoraAsc(
            Long pacienteId, Collection<AppointmentStatus> estados, LocalDateTime desde);

    List<Cita> findBySucursalIdAndEstadoAndFechaHoraBetweenOrderByHoraLlegadaAsc(
            Long sucursalId, AppointmentStatus estado, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findByMedicoUsernameAndEstadoAndFechaHoraBetweenOrderByHoraLlegadaAsc(
            String username, AppointmentStatus estado, LocalDateTime desde, LocalDateTime hasta);

    @EntityGraph(attributePaths = {"paciente", "especialidad", "sucursal"})
    List<Cita> findByMedicoIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNotInOrderByFechaHoraAsc(
            Long medicoId, LocalDateTime desde, LocalDateTime hasta, Collection<AppointmentStatus> excluidos);

    Optional<Cita> findFirstByConsultaOrigenIdAndEstadoNot(Long consultaId, AppointmentStatus estado);

    List<Cita> findByTipoSeguimientoIsNotNullAndRecordatorioEnviadoFalseAndEstadoInAndFechaHoraBetween(
            Collection<AppointmentStatus> estados, LocalDateTime desde, LocalDateTime hasta);

}