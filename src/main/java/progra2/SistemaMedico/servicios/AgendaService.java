package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.EventoAgenda;
import progra2.SistemaMedico.model.TareaAgenda;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.PrioridadTarea;
import progra2.SistemaMedico.model.enums.TipoEvento;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.repositorio.EventoAgendaRepositorio;
import progra2.SistemaMedico.repositorio.TareaAgendaRepositorio;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgendaService {

    public static final String COLOR_EVENTO = "#8b5cf6";
    private static final int MINUTOS_POR_CITA = 30;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ISO_MINUTOS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // FB PASO 2 - CU-16 INICIO
    // Paso 2: no se dibujan las pendientes de pago, las de inasistencia ni las canceladas
    private static final Set<AppointmentStatus> CITAS_OCULTAS = EnumSet.of(
            AppointmentStatus.PENDIENTE_PAGO, AppointmentStatus.NO_ASISTIO, AppointmentStatus.CANCELADA);
    private static final Set<AppointmentStatus> CITAS_SIN_CONFLICTO = EnumSet.of(
            AppointmentStatus.CANCELADA, AppointmentStatus.NO_ASISTIO, AppointmentStatus.ATENCION_FINALIZADA);
    private static final Map<AppointmentStatus, String> COLOR_CITA = Map.of(
            AppointmentStatus.PAGADA, "#0d6efd",
            AppointmentStatus.REPROGRAMADA, "#6c757d",
            AppointmentStatus.PACIENTE_PRESENTE, "#0dcaf0",
            AppointmentStatus.SIGNOS_VITALES, "#20c997",
            AppointmentStatus.EN_ESPERA, "#ffc107",
            AppointmentStatus.CONSULTA_MEDICA, "#fd7e14",
            AppointmentStatus.EVALUADO, "#d63384",
            AppointmentStatus.ATENCION_FINALIZADA, "#198754");
    // FB PASO 2 - CU-16 FINAL

    private final EventoAgendaRepositorio eventoRepositorio;
    private final TareaAgendaRepositorio tareaRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 1 - CU-16 INICIO
    // Precondición: la agenda es personal, solo existe para usuarios con rol Médico
    @Transactional(readOnly = true)
    public Usuario medico(String username) {
        return usuarioRepositorio.findByUsername(username)
                .filter(u -> u.getRol() == UserRol.MEDICO)
                .orElseThrow(() -> new ReglasDeNegocioException(
                        "La agenda médica es personal: solo la pueden usar los usuarios con rol Médico."));
    }
    // FB PASO 1 - CU-16 FINAL

    // ============================================================
    //  CALENDARIO (paso 2, FA01, FA05)
    // ============================================================

    // FB PASO 2 / FA05 - CU-16 INICIO
    @Transactional(readOnly = true)
    public List<EventoCalendarioDTO> calendario(String username, LocalDateTime desde, LocalDateTime hasta) {
        Long medicoId = medico(username).getId();
        List<EventoCalendarioDTO> resultado = new ArrayList<>();
        eventoRepositorio.findByMedicoIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(medicoId, hasta, desde)
                .forEach(e -> resultado.add(aCalendario(e)));
        citaRepositorio.findByMedicoIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNotInOrderByFechaHoraAsc(
                        medicoId, desde, hasta, CITAS_OCULTAS)
                .forEach(c -> resultado.add(aCalendario(c)));
        return resultado;
    }
    // FB PASO 2 / FA05 - CU-16 FINAL

    // ============================================================
    //  EVENTOS (pasos 3 a 9, FA03, FA04, FA07)
    // ============================================================

    // FB PASOS 6-8 / FA07 - CU-16 INICIO
    @Transactional
    public RespuestaAgendaDTO guardarEvento(EventoFormDTO form, String username) {
        Usuario medico = medico(username);
        // FA07 - CU-16 INICIO
        EventoAgenda evento = form.getId() == null ? new EventoAgenda()
                : eventoRepositorio.findByIdAndMedicoId(form.getId(), medico.getId())
                        .orElseThrow(() -> new ReglasDeNegocioException("El evento ya no existe."));
        // FA07 - CU-16 FINAL
        boolean nuevo = evento.getId() == null;

        // FB PASO 6 - CU-16 INICIO
        Map<String, String> errores = new LinkedHashMap<>();
        String titulo = texto(form.getTitulo());
        if (titulo == null) {
            errores.put("titulo", "El título es obligatorio.");
        } else if (titulo.length() < 5 || titulo.length() > 200) {
            errores.put("titulo", "El título debe tener entre 5 y 200 caracteres.");
        }
        TipoEvento tipo = enumValor(TipoEvento.class, form.getTipo());
        if (tipo == null) {
            errores.put("tipo", "Debe seleccionar el tipo de evento.");
        }
        String descripcion = texto(form.getDescripcion());
        if (descripcion != null && descripcion.length() > 500) {
            errores.put("descripcion", "La descripción no puede exceder 500 caracteres.");
        }

        // Todo el día: se guarda de 00:00 del primer día a 00:00 del día siguiente al último (fin exclusivo)
        LocalDateTime inicio = form.isTodoElDia()
                ? Optional.ofNullable(fecha(form.getInicio(), "inicio", "de inicio", errores)).map(LocalDate::atStartOfDay).orElse(null)
                : fechaHora(form.getInicio(), "inicio", "de inicio", errores);
        LocalDateTime fin = form.isTodoElDia()
                ? Optional.ofNullable(fecha(form.getFin(), "fin", "de fin", errores)).map(d -> d.plusDays(1).atStartOfDay()).orElse(null)
                : fechaHora(form.getFin(), "fin", "de fin", errores);
        if (inicio != null && fin != null && !fin.isAfter(inicio)) {
            errores.put("fin", "La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (nuevo && inicio != null && inicio.toLocalDate().isBefore(LocalDate.now())) {
            errores.put("inicio", "La fecha de inicio no puede ser anterior a hoy.");
        }
        // FB PASO 6 - CU-16 FINAL
        // FA03 - CU-16 INICIO
        if (!errores.isEmpty()) {
            return RespuestaAgendaDTO.errores(errores);
        }
        // FA03 - CU-16 FINAL

        // FB PASO 7 - CU-16 INICIO
        evento.setMedico(medico);
        evento.setTitulo(titulo);
        evento.setDescripcion(descripcion);
        evento.setTipo(tipo);
        evento.setInicio(inicio);
        evento.setFin(fin);
        evento.setTodoElDia(form.isTodoElDia());
        eventoRepositorio.save(evento);
        // FB PASO 7 - CU-16 FINAL

        auditoriaService.registrar(nuevo ? "EVENTO_CREADO" : "EVENTO_EDITADO", "EventoAgenda", evento.getId(),
                tipo.getDescripcion() + " \"" + titulo + "\" de " + username + ": "
                        + inicio.format(FECHA_HORA) + " a " + fin.format(FECHA_HORA));

        // FB PASO 8 - CU-16 INICIO
        String mensaje = nuevo
                ? "Evento creado exitosamente. " + titulo + " — " + inicio.format(FECHA) + " a " + ultimoDia(evento).format(FECHA) + "."
                : "Evento actualizado exitosamente.";
        // FB PASO 8 - CU-16 FINAL
        long choques = citasEnRango(medico.getId(), inicio, fin);
        String aviso = choques == 0 ? null
                : "Atención: hay " + choques + " cita(s) programadas en ese rango; coordine con recepción para reasignarlas.";
        return RespuestaAgendaDTO.exito(mensaje, aviso);
    }
    // FB PASOS 6-8 / FA07 - CU-16 FINAL

    // FA04 - CU-16 INICIO
    @Transactional
    public RespuestaAgendaDTO eliminarEvento(Long id, String username) {
        Usuario medico = medico(username);
        EventoAgenda evento = eventoRepositorio.findByIdAndMedicoId(id, medico.getId())
                .orElseThrow(() -> new ReglasDeNegocioException("El evento ya no existe."));
        eventoRepositorio.delete(evento);
        auditoriaService.registrar("EVENTO_ELIMINADO", "EventoAgenda", id,
                "\"" + evento.getTitulo() + "\" de " + username);
        return RespuestaAgendaDTO.exito("Evento eliminado exitosamente.");
    }
    // FA04 - CU-16 FINAL

    // ============================================================
    //  TAREAS (FA06)
    // ============================================================

    // FA06 - CU-16 INICIO
    @Transactional(readOnly = true)
    public TareasDelDiaDTO tareas(String username, LocalDate fecha, String filtro) {
        List<TareaAgenda> delDia = tareaRepositorio.findByMedicoIdAndFechaLimite(medico(username).getId(), fecha).stream()
                .sorted(Comparator.comparing(TareaAgenda::getCompletada)
                        .thenComparing(TareaAgenda::getPrioridad, Comparator.reverseOrder())
                        .thenComparing(TareaAgenda::getId))
                .toList();
        long completadas = delDia.stream().filter(TareaAgenda::getCompletada).count();
        List<TareaDTO> visibles = delDia.stream()
                .filter(t -> switch (filtro == null ? "" : filtro) {
                    case "pendientes" -> !t.getCompletada();
                    case "completadas" -> t.getCompletada();
                    default -> true;
                })
                .map(this::aTarea)
                .toList();
        return new TareasDelDiaDTO(fecha.toString(), fecha.format(FECHA), delDia.size(),
                delDia.size() - completadas, completadas, visibles);
    }

    @Transactional
    public RespuestaAgendaDTO crearTarea(TareaFormDTO form, String username) {
        Usuario medico = medico(username);
        Map<String, String> errores = new LinkedHashMap<>();
        String titulo = texto(form.getTitulo());
        if (titulo == null) {
            errores.put("titulo", "El título es obligatorio.");
        } else if (titulo.length() < 5 || titulo.length() > 200) {
            errores.put("titulo", "El título debe tener entre 5 y 200 caracteres.");
        }
        PrioridadTarea prioridad = enumValor(PrioridadTarea.class, form.getPrioridad());
        if (prioridad == null) {
            errores.put("prioridad", "Debe seleccionar la prioridad.");
        }
        LocalDate limite = fecha(form.getFechaLimite(), "fechaLimite", "límite", errores);
        if (limite != null && limite.isBefore(LocalDate.now())) {
            errores.put("fechaLimite", "La fecha límite no puede ser anterior a hoy.");
        }
        String descripcion = texto(form.getDescripcion());
        if (descripcion != null && descripcion.length() > 1000) {
            errores.put("descripcion", "La descripción no puede exceder 1000 caracteres.");
        }
        if (!errores.isEmpty()) {
            return RespuestaAgendaDTO.errores(errores);
        }
        tareaRepositorio.save(TareaAgenda.builder()
                .medico(medico)
                .titulo(titulo)
                .descripcion(descripcion)
                .prioridad(prioridad)
                .fechaLimite(limite)
                .completada(false)
                .build());
        return RespuestaAgendaDTO.exito("Tarea creada exitosamente para el " + limite.format(FECHA) + ".");
    }

    @Transactional
    public RespuestaAgendaDTO cambiarEstadoTarea(Long id, String username) {
        TareaAgenda tarea = tareaDe(id, username);
        tarea.setCompletada(!tarea.getCompletada());
        return RespuestaAgendaDTO.exito(tarea.getCompletada() ? "Tarea completada." : "Tarea marcada como pendiente.");
    }

    @Transactional
    public RespuestaAgendaDTO eliminarTarea(Long id, String username) {
        tareaRepositorio.delete(tareaDe(id, username));
        return RespuestaAgendaDTO.exito("Tarea eliminada.");
    }
    // FA06 - CU-16 FINAL

    // ============================================================
    //  APOYO
    // ============================================================

    private TareaAgenda tareaDe(Long id, String username) {
        return tareaRepositorio.findByIdAndMedicoId(id, medico(username).getId())
                .orElseThrow(() -> new ReglasDeNegocioException("La tarea ya no existe."));
    }

    // Una cita [h, h + 30 min) choca con el evento [inicio, fin) si empieza antes del fin y termina después del inicio
    private long citasEnRango(Long medicoId, LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime ahora = LocalDateTime.now();
        return citaRepositorio.findByMedicoIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNotInOrderByFechaHoraAsc(
                        medicoId, inicio.minusMinutes(MINUTOS_POR_CITA - 1), fin, CITAS_SIN_CONFLICTO)
                .stream()
                .filter(c -> c.getEstado() != AppointmentStatus.PENDIENTE_PAGO
                        || c.getFechaReservaExpira() == null || c.getFechaReservaExpira().isAfter(ahora))
                .count();
    }

    private LocalDate ultimoDia(EventoAgenda e) {
        return e.getTodoElDia() ? e.getFin().toLocalDate().minusDays(1) : e.getFin().toLocalDate();
    }

    // FB PASO 2 / FA01 - CU-16 INICIO
    private EventoCalendarioDTO aCalendario(EventoAgenda e) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("clase", "evento");
        datos.put("eventoId", e.getId());
        datos.put("tipo", e.getTipo().name());
        datos.put("tipoDescripcion", e.getTipo().getDescripcion());
        datos.put("descripcion", e.getDescripcion());
        datos.put("todoElDia", e.getTodoElDia());
        datos.put("inicioTexto", e.getTodoElDia() ? e.getInicio().format(FECHA) : e.getInicio().format(FECHA_HORA));
        datos.put("finTexto", e.getTodoElDia() ? ultimoDia(e).format(FECHA) : e.getFin().format(FECHA_HORA));
        datos.put("inicioForm", e.getTodoElDia() ? e.getInicio().toLocalDate().toString() : e.getInicio().format(ISO_MINUTOS));
        datos.put("finForm", e.getTodoElDia() ? ultimoDia(e).toString() : e.getFin().format(ISO_MINUTOS));
        String inicio = e.getTodoElDia() ? e.getInicio().toLocalDate().toString() : e.getInicio().format(ISO_MINUTOS);
        String fin = e.getTodoElDia() ? e.getFin().toLocalDate().toString() : e.getFin().format(ISO_MINUTOS);
        return new EventoCalendarioDTO("e-" + e.getId(), e.getTitulo(), inicio, fin, e.getTodoElDia(), COLOR_EVENTO, datos);
    }

    private EventoCalendarioDTO aCalendario(Cita c) {
        String paciente = c.getPaciente().getNombre() + " " + c.getPaciente().getApellidos();
        LocalDateTime fin = c.getFechaHora().plusMinutes(MINUTOS_POR_CITA);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("clase", "cita");
        datos.put("numero", c.getNumeroCita());
        datos.put("paciente", paciente);
        datos.put("estado", c.getEstado().getDescripcion());
        datos.put("especialidad", c.getEspecialidad().getNombre());
        datos.put("sede", c.getSucursal().getNombre());
        datos.put("descripcion", c.getMotivoConsulta());
        datos.put("inicioTexto", c.getFechaHora().format(FECHA_HORA));
        datos.put("finTexto", fin.format(FECHA_HORA));
        return new EventoCalendarioDTO("c-" + c.getId(), "Cita: " + paciente, c.getFechaHora().format(ISO_MINUTOS),
                fin.format(ISO_MINUTOS), false, COLOR_CITA.getOrDefault(c.getEstado(), "#6c757d"), datos);
    }
    // FB PASO 2 / FA01 - CU-16 FINAL

    private TareaDTO aTarea(TareaAgenda t) {
        return new TareaDTO(t.getId(), t.getTitulo(), t.getDescripcion(), t.getPrioridad().name(),
                t.getPrioridad().getDescripcion(), t.getPrioridad().getColor(), t.getFechaLimite().format(FECHA),
                t.getCompletada());
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private <E extends Enum<E>> E enumValor(Class<E> tipo, String valor) {
        try {
            return valor == null || valor.isBlank() ? null : Enum.valueOf(tipo, valor.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDate fecha(String valor, String campo, String nombre, Map<String, String> errores) {
        if (texto(valor) == null) {
            errores.put(campo, "La fecha " + nombre + " es obligatoria.");
            return null;
        }
        try {
            return LocalDate.parse(valor.trim().substring(0, Math.min(10, valor.trim().length())));
        } catch (DateTimeParseException e) {
            errores.put(campo, "La fecha " + nombre + " no es válida.");
            return null;
        }
    }

    private LocalDateTime fechaHora(String valor, String campo, String nombre, Map<String, String> errores) {
        if (texto(valor) == null) {
            errores.put(campo, "La fecha " + nombre + " es obligatoria.");
            return null;
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            errores.put(campo, "La fecha " + nombre + " debe incluir fecha y hora.");
            return null;
        }
    }
}
