package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.PreparacionSeguimientoDTO;
import progra2.SistemaMedico.dto.SeguimientoCorreoDTO;
import progra2.SistemaMedico.dto.SeguimientoFormDTO;
import progra2.SistemaMedico.excepciones.HorarioNoDisponibleException;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Consulta;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.repositorio.ConsultaRepositorio;
import progra2.SistemaMedico.servicios.serviceImpl.ICitaService;
import progra2.SistemaMedico.servicios.serviceImpl.IEmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeguimientoService {

    private static final String MENSAJE_FECHA =
            "Seleccione una fecha futura dentro de los horarios disponibles del médico.";
    private static final String MENSAJE_OBSERVACIONES =
            "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.";
    private static final List<AppointmentStatus> CON_RECORDATORIO =
            List.of(AppointmentStatus.PENDIENTE_PAGO, AppointmentStatus.PAGADA);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MedicoService medicoService;
    private final ICitaService citaService;
    private final CitaRepositorio citaRepositorio;
    private final ConsultaRepositorio consultaRepositorio;
    private final AuditoriaService auditoriaService;
    private final IEmailService emailService;

    // FB PASOS 2-3 - CU-12 INICIO
    @Transactional(readOnly = true)
    public PreparacionSeguimientoDTO preparar(Long citaId, LocalDate fecha, String username) {
        Cita cita = medicoService.buscarEnConsulta(citaId, username);
        Consulta consulta = consultaSinSeguimiento(cita);
        LocalDate dia = esFutura(fecha) ? fecha : primerDia();

        return PreparacionSeguimientoDTO.builder()
                .citaId(cita.getId())
                .numeroCita(cita.getNumeroCita())
                .consultaId(consulta.getId())
                .nombrePaciente(nombreCompleto(cita.getPaciente()))
                .emailPaciente(cita.getPaciente().getEmail())
                .nombreMedico(nombreCompleto(cita.getMedico()))
                .nombreEspecialidad(cita.getEspecialidad().getNombre())
                .nombreSucursal(cita.getSucursal().getNombre())
                .fecha(dia)
                .primerDia(primerDia())
                .horarios(citaService.obtenerHorariosDisponibles(cita.getMedico().getId(), dia))
                .build();
    }
    // FB PASOS 2-3 - CU-12 FINAL

    // FB PASO 5 - CU-12 INICIO
    @Transactional(readOnly = true)
    public List<String> horarios(Long citaId, LocalDate fecha, String username) {
        Cita cita = medicoService.buscarEnConsulta(citaId, username);
        if (!esFutura(fecha)) {
            return List.of();
        }
        return citaService.obtenerHorariosDisponibles(cita.getMedico().getId(), fecha);
    }
    // FB PASO 5 - CU-12 FINAL

    // FB PASOS 6-9 - CU-12 INICIO
    @Transactional
    public String agendar(Long citaId, SeguimientoFormDTO form, String username) {
        Cita origen = medicoService.buscarEnConsulta(citaId, username);
        Consulta consulta = consultaSinSeguimiento(origen);

        // FB PASO 4 - CU-12 INICIO
        if (form.getTipo() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar el tipo de seguimiento.");
        }
        // FB PASO 4 - CU-12 FINAL
        // FB PASO 6 - CU-12 INICIO
        String observaciones = form.getObservaciones() == null ? "" : form.getObservaciones().trim();
        if (observaciones.length() < 10 || observaciones.length() > 2000) {
            throw new ReglasDeNegocioException(MENSAJE_OBSERVACIONES);
        }
        // FB PASO 6 - CU-12 FINAL
        LocalDateTime fechaHora = fechaHora(form);
        Long medicoId = origen.getMedico().getId();

        // FA01 - CU-12 INICIO
        if (citaRepositorio.findHorariosOcupados(medicoId, fechaHora.toLocalDate()).contains(fechaHora)) {
            throw new HorarioNoDisponibleException();
        }
        // FA01 - CU-12 FINAL
        if (!citaService.obtenerHorariosDisponibles(medicoId, fechaHora.toLocalDate())
                .contains(fechaHora.toLocalTime().toString())) {
            throw new ReglasDeNegocioException(MENSAJE_FECHA);
        }

        // FB PASO 7 - CU-12 INICIO
        Cita seguimiento = Cita.builder()
                .numeroCita(CitaService.generarNumeroCita())
                .paciente(origen.getPaciente())
                .medico(origen.getMedico())
                .especialidad(origen.getEspecialidad())
                .sucursal(origen.getSucursal())
                .fechaHora(fechaHora)
                .motivoConsulta(observaciones)
                .estado(AppointmentStatus.PENDIENTE_PAGO)
                .fechaReservaExpira(null)
                .prioridad(form.getPrioridad() == null ? Prioridad.NORMAL : form.getPrioridad())
                .tipoSeguimiento(form.getTipo())
                .consultaOrigen(consulta)
                .recordatorioEnviado(false)
                .build();
        citaRepositorio.saveAndFlush(seguimiento);
        // FB PASO 7 - CU-12 FINAL

        auditoriaService.registrar("SEGUIMIENTO_AGENDADO", "Cita", seguimiento.getId(),
                form.getTipo().getDescripcion() + " · " + fechaHora.format(FORMATO)
                        + " · consulta de la cita " + origen.getNumeroCita());

        // FB PASO 9 - CU-12 INICIO
        emailService.enviarSeguimientoAgendado(aCorreo(seguimiento));
        // FB PASO 9 - CU-12 FINAL

        // FB PASO 8 - CU-12 INICIO
        return "Cita de seguimiento agendada exitosamente. Tipo: " + form.getTipo().getDescripcion()
                + ". Paciente: " + nombreCompleto(origen.getPaciente()) + ".";
        // FB PASO 8 - CU-12 FINAL
    }
    // FB PASOS 6-9 - CU-12 FINAL

    // FB PASO 10 - CU-12 INICIO
    @Transactional
    public int enviarRecordatorios(LocalDate dia) {
        List<Cita> citas = citaRepositorio
                .findByTipoSeguimientoIsNotNullAndRecordatorioEnviadoFalseAndEstadoInAndFechaHoraBetween(
                        CON_RECORDATORIO, dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
        int enviados = 0;
        for (Cita cita : citas) {
            if (emailService.enviarRecordatorioSeguimiento(aCorreo(cita))) {
                cita.setRecordatorioEnviado(true);
                enviados++;
            }
        }
        return enviados;
    }
    // FB PASO 10 - CU-12 FINAL

    private Consulta consultaSinSeguimiento(Cita cita) {
        Consulta consulta = consultaRepositorio.findByCitaId(cita.getId())
                .orElseThrow(() -> new ReglasDeNegocioException(
                        "Guarde la consulta al menos una vez antes de agendar un seguimiento."));
        citaRepositorio.findFirstByConsultaOrigenIdAndEstadoNot(consulta.getId(), AppointmentStatus.CANCELADA).ifPresent(s -> {
            throw new ReglasDeNegocioException("Esta consulta ya tiene una cita de seguimiento: "
                    + s.getNumeroCita() + " para el " + s.getFechaHora().format(FORMATO) + ".");
        });
        return consulta;
    }

    private LocalDateTime fechaHora(SeguimientoFormDTO form) {
        if (!esFutura(form.getFecha()) || form.getHora() == null) {
            throw new ReglasDeNegocioException(MENSAJE_FECHA);
        }
        try {
            return LocalDateTime.of(form.getFecha(), LocalTime.parse(form.getHora()));
        } catch (DateTimeParseException e) {
            throw new ReglasDeNegocioException(MENSAJE_FECHA);
        }
    }

    private SeguimientoCorreoDTO aCorreo(Cita cita) {
        return new SeguimientoCorreoDTO(
                cita.getId(),
                cita.getNumeroCita(),
                nombreCompleto(cita.getPaciente()),
                cita.getPaciente().getEmail(),
                cita.getFechaHora(),
                cita.getTipoSeguimiento().getDescripcion(),
                nombreCompleto(cita.getMedico()),
                cita.getEspecialidad().getNombre(),
                cita.getSucursal().getNombre(),
                cita.getMotivoConsulta(),
                cita.getEstado() == AppointmentStatus.PENDIENTE_PAGO);
    }

    private boolean esFutura(LocalDate fecha) {
        return fecha != null && fecha.isAfter(LocalDate.now());
    }

    private LocalDate primerDia() {
        return LocalDate.now().plusDays(1);
    }

    private String nombreCompleto(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellidos();
    }
}
