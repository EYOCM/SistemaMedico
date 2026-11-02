package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.ResultadoBusquedaCitaDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.repositorio.CitaRepositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecepcionService {
    private final CitaMapper citaMapper;
    private final CitaRepositorio citaRepositorio;
    private final AuditoriaService auditoriaService;
    private final BusquedaCitaService busquedaCitaService;
    private static final List<AppointmentStatus> ESTADOS_ACTIVOS = List.of(
            AppointmentStatus.PENDIENTE_PAGO,
            AppointmentStatus.PAGADA,
            AppointmentStatus.PACIENTE_PRESENTE
    );

    // FB PASO 3 - CU-05 INICIO
    public ResultadoBusquedaCitaDTO buscarPorNumero(String numero) {
        return busquedaCitaService.buscarPorNumero(numero, EnumSet.allOf(AppointmentStatus.class));
    }
    // FB PASO 3 - CU-05 FINAL

    // FA02 - CU-05 INICIO
    public ResultadoBusquedaCitaDTO buscarPorDpi(String dpi) {
        return busquedaCitaService.buscarPorDpi(dpi, ESTADOS_ACTIVOS);
    }
    // FA02 - CU-05 FINAL

    // FB PASOS 6-7 - CU-05 INICIO
    @Transactional
    public CitaDTO registrarLlegada(Long citaId){
        Cita cita = citaRepositorio.findById(citaId).orElseThrow(() -> new ReglasDeNegocioException("No se encontró la cita."));
// FA09 - CU-05 INICIO
//problema al registrar la llegada FA09 1
        if(cita.getEstado() == AppointmentStatus.PACIENTE_PRESENTE){
            throw new ReglasDeNegocioException("Operación no permitida: la llegada de este paciente ya fue registrada.");
        }
        // FA09 - CU-05 FINAL
        // FA05 - CU-05 INICIO
        if(cita.getEstado() == AppointmentStatus.PENDIENTE_PAGO){
            throw new ReglasDeNegocioException("La cita del paciente tiene estado 'Pendiente de pago'. Debe realizar el pago en caja antes de ser atendido.");
        }
        // FA05 - CU-05 FINAL
        // FA06 - CU-05 INICIO
        if(cita.getEstado() == AppointmentStatus.CANCELADA){
            throw new ReglasDeNegocioException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }
        // FA06 - CU-05 FINAL
        // FA09 - CU-05 INICIO
        if(cita.getEstado() != AppointmentStatus.PAGADA){
            throw new ReglasDeNegocioException("Operación no permitida.");
        }
        if(!cita.getFechaHora().toLocalDate().equals(LocalDate.now())){
            throw new ReglasDeNegocioException("Solo se puede registrar la llegada el día de la cita.");
        }
        // FA09 - CU-05 FINAL
        // FB PASO 7 - CU-05 INICIO
        //cambio de estado, registrar llegada y guardar
        cita.setEstado(AppointmentStatus.PACIENTE_PRESENTE);
        cita.setHoraLlegada(LocalDateTime.now());
        citaRepositorio.saveAndFlush(cita);

        auditoriaService.registrar("LLEGADA", "Cita", cita.getId(),
                "Llegada registrada. Prioridad " + cita.getPrioridad());
                // FB PASO 7 - CU-05 FINAL
        return citaMapper.convertirADTO(cita);
    }
    // FB PASOS 6-7 - CU-05 FINAL
}
