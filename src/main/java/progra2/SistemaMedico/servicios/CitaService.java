package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Especialidad;
import progra2.SistemaMedico.model.EventoAgenda;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.Sucursal;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.repositorio.EspecialidadRepositorio;
import progra2.SistemaMedico.repositorio.EventoAgendaRepositorio;
import progra2.SistemaMedico.repositorio.SucursalRepositorio;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;
import progra2.SistemaMedico.servicios.serviceImpl.ICitaService;
import progra2.SistemaMedico.util.ValidacionDpi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService implements ICitaService {

    private final CitaRepositorio citaRepo;
    private final UsuarioRepositorio userRepo;
    private final SucursalRepositorio sucursalRepo;
    private final EspecialidadRepositorio especialidadRepo;
    private final CatalogoService catalogoService;
    private final CitaMapper mapper;
    private final AuditoriaService auditoria;
    private final PasswordEncoder passwordEncoder;
    private final EventoAgendaRepositorio eventoRepo;

    /** Horario de atención del hospital y duración de cada espacio. */
    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORA_CIERRE   = LocalTime.of(16, 0);
    private static final int MINUTOS_POR_CITA    = 30;

    /** Minutos que el paciente tiene para pagar antes de que se libere el horario (RNF-019). */
    private static final int MINUTOS_RESERVA = 5;

    // ============================================================
    //  CATÁLOGOS
    // ============================================================


    // FB PASO 2 - CU-03 INICIO
    @Override
    public List<SucursalDTO> obtenerSucursalesActivas() {
        return catalogoService.sucursalesActivas().stream()
                .map(s -> new SucursalDTO(
                        s.getId(), s.getNombre(), s.getDireccion(), s.getTelefono(),
                        s.getEmail(), s.getActivo(), s.getFechaCreacion(), s.getFechaActualizacion()))
                .collect(Collectors.toList());
    }
    // FB PASO 2 - CU-03 FINAL


    // FB PASO 3 - CU-03 INICIO
    @Override
    @Cacheable("especialidadesPorSucursal")
    public List<EspecialidadDTO> obtenerEspecialidadesPorSucursal(Long sucursalId) {
        System.out.println("[BD] Leyendo especialidades de la sucursal " + sucursalId);
        List<Especialidad> especialidades = userRepo.findEspecialidadesDisponiblesPorSucursal(sucursalId);
        return especialidades.stream()
                .map(e -> new EspecialidadDTO(
                        e.getId(), e.getNombre(), e.getDescripcion(),
                        e.getActivo(), e.getFechaCreacion(), e.getFechaActualizacion()))
                .collect(Collectors.toList());
    }
    // FB PASO 3 - CU-03 FINAL

    // FB PASO 4 - CU-03 INICIO
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerMedicosDisponibles(Long sucursalId, Long especialidadId) {
        return userRepo.findMedicoBySucursalYEspecialidad(sucursalId, especialidadId)
                .stream()
                .map(u -> UsuarioDTO.builder()
                        .id(u.getId())
                        .rol(u.getRol())
                        .dpi(u.getDpi())
                        .nit(u.getNit())
                        .nombre(u.getNombre())
                        .apellidos(u.getApellidos())
                        .username(u.getUsername())
                        .build())
                .collect(Collectors.toList());
    }
    // FB PASO 4 - CU-03 FINAL

    // ============================================================
    //  DISPONIBILIDAD
    // ==========================================================


     // FB PASO 5 - CU-03 INICIO
     //espacios libres del médico en una fecha.

    @Override
    @Transactional(readOnly = true)
    public List<String> obtenerHorariosDisponibles(Long medicoId, LocalDate fecha) {

        List<String> disponibles = new ArrayList<>();
        LocalTime hora = HORA_APERTURA;
        while (hora.isBefore(HORA_CIERRE)) {
            disponibles.add(hora.toString());
            hora = hora.plusMinutes(MINUTOS_POR_CITA);
        }

        // Quitar los espacios ya ocupados (la consulta ya excluye canceladas y vencidas)
        List<String> ocupados = citaRepo.findHorariosOcupados(medicoId, fecha).stream()
                .map(o -> o.toLocalTime().toString())
                .collect(Collectors.toList());
        disponibles.removeAll(ocupados);

        // CU-16: quitar los espacios que caen dentro de un evento de la agenda del médico
        List<EventoAgenda> eventos = eventosDelDia(medicoId, fecha);
        disponibles.removeIf(h -> bloqueadoPorEvento(eventos, fecha.atTime(LocalTime.parse(h))));

        // Si la fecha es hoy, quitar las horas que ya pasaron
        if (fecha.isEqual(LocalDate.now())) {
            LocalTime ahora = LocalTime.now();
            disponibles.removeIf(h -> LocalTime.parse(h).isBefore(ahora));
        }

        return disponibles;
    }
    // FB PASO 5 - CU-03 FINAL

    // ============================================================
    //  AGENDAMIENTO
    // ============================================================

    // FB PASOS 8-9 - CU-03 INICIO
    // registra la cita en estado "Pendiente de pago".


    @Override
    @Transactional
    public CitaDTO agendarCita(ApiResponse.SolicitudCitaDTO solicitud) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario paciente = userRepo.findByUsername(username)
                .orElseThrow(() -> new ReglasDeNegocioException("Usuario autenticado no encontrado."));
        return crearCita(paciente, solicitud, LocalDateTime.now().plusMinutes(MINUTOS_RESERVA));
    }
    // FB PASOS 8-9 - CU-03 FINAL

    // FA04 / FA06 - CU-05 INICIO
    // CU-05 FA04 / FA06: la recepcionista agenda a nombre del paciente que llega sin cita.
    @Transactional
    public CitaDTO agendarWalkIn(String dpi, ApiResponse.SolicitudCitaDTO solicitud, String recepcionista) {
        CitaDTO cita = crearCita(pacientePorDpi(dpi), solicitud, null);
        auditoria.registrar("CITA_WALK_IN", "Cita", cita.getId(),
                "Cita " + cita.getNumeroCita() + " agendada en recepción por " + recepcionista);
        return cita;
    }
    // FA04 / FA06 - CU-05 FINAL

    // FA04 / FA06 - CU-05 INICIO
    @Transactional(readOnly = true)
    public PreparacionWalkInDTO prepararWalkIn(String dpi, String recepcionista) {
        Usuario paciente = pacientePorDpi(dpi);
        Long sucursalId = userRepo.findByUsername(recepcionista)
                .map(Usuario::getSucursal)
                .map(Sucursal::getId)
                .orElse(null);
        return new PreparacionWalkInDTO(paciente.getDpi(),
                paciente.getNombre() + " " + paciente.getApellidos(), sucursalId);
    }
    // FA04 / FA06 - CU-05 FINAL

    // FA04 / FA06 - CU-05 INICIO
    public List<OpcionDTO> opcionesEspecialidades(Long sucursalId) {
        return obtenerEspecialidadesPorSucursal(sucursalId).stream()
                .map(e -> new OpcionDTO(e.getId(), e.getNombre()))
                .collect(Collectors.toList());
    }

    public List<OpcionDTO> opcionesMedicos(Long sucursalId, Long especialidadId) {
        return obtenerMedicosDisponibles(sucursalId, especialidadId).stream()
                .map(m -> new OpcionDTO(m.getId(), m.getNombre() + " " + m.getApellidos()))
                .collect(Collectors.toList());
    }
    // FA04 / FA06 - CU-05 FINAL

    // FA04 / FA06 - CU-05 INICIO
    private Usuario pacientePorDpi(String dpi) {
        String valor = dpi == null ? "" : dpi.trim();
        return userRepo.findByDpi(valor)
                .filter(u -> u.getRol() == UserRol.PACIENTE && Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró ningún paciente activo con ese DPI."));
    }
    // FA04 / FA06 - CU-05 FINAL

    // FB PASO 9 - CU-03 INICIO
    private CitaDTO crearCita(Usuario paciente, ApiResponse.SolicitudCitaDTO solicitud, LocalDateTime venceReserva) {
        validarSolicitud(solicitud);


        Usuario medico = userRepo.findByIdParaAgendar(solicitud.getMedicoId())
                .orElseThrow(() -> new ReglasDeNegocioException("El médico seleccionado no existe."));
        validarMedico(medico, solicitud.getSucursalId(), solicitud.getEspecialidadId());

        if (horarioOcupado(medico.getId(), solicitud.getFechaHora())) {
            throw new ReglasDeNegocioException(
                    "El horario seleccionado acaba de ser ocupado por otro paciente. Por favor, elija otro horario.");
        }

        Cita nuevaCita = Cita.builder()
                .numeroCita(generarNumeroCita())
                .paciente(paciente)
                .medico(medico)
                .sucursal(sucursalRepo.findById(solicitud.getSucursalId())
                        .orElseThrow(() -> new ReglasDeNegocioException("La sucursal seleccionada no existe.")))
                .especialidad(especialidadRepo.findById(solicitud.getEspecialidadId())
                        .orElseThrow(() -> new ReglasDeNegocioException("La especialidad seleccionada no existe.")))
                .fechaHora(solicitud.getFechaHora())
                .motivoConsulta(solicitud.getMotivoCita().trim())
                .estado(AppointmentStatus.PENDIENTE_PAGO)          // postcondición del CU-03
                .fechaReservaExpira(venceReserva)
                .build();

        return mapper.convertirADTO(citaRepo.save(nuevaCita));
    }
    // FB PASO 9 - CU-03 FINAL

    // FB PASOS 2-7 - CU-03 INICIO
    /** Reglas RN-CU03-01 a RN-CU03-05, validadas del lado del servidor. */
    private void validarSolicitud(ApiResponse.SolicitudCitaDTO s) {
        if (s.getSucursalId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una sucursal para continuar.");
        }
        if (s.getEspecialidadId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una especialidad médica para continuar.");
        }
        if (s.getMedicoId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar un médico para continuar.");
        }
        // RN-CU03-05 (fecha y hora): obligatoria y futura
        if (s.getFechaHora() == null || !s.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ReglasDeNegocioException(
                    "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");
        }
        validarMotivo(s.getMotivoCita());
    }
    // FB PASOS 2-7 - CU-03 FINAL

    // FB PASO 7 - CU-03 INICIO
    // RN-CU03-03 (motivo de visita): 10 a 2000 caracteres
    private void validarMotivo(String texto) {
        String motivo = texto == null ? "" : texto.trim();
        if (motivo.length() < 10 || motivo.length() > 2000) {
            throw new ReglasDeNegocioException(
                    "El motivo debe contener entre 10 y 2000 caracteres. Usted ingresó " + motivo.length() + " caracteres.");
        }
    }
    // FB PASO 7 - CU-03 FINAL


    public static String generarNumeroCita() {
        String fecha = LocalDate.now().toString().replace("-", "");
        int aleatorio = (int) (Math.random() * 9000) + 1000;
        return "CITA-" + fecha + "-" + aleatorio;
    }

    // FB PASO 15 - CU-04 INICIO
    @Transactional(readOnly = true)
    public List<CitaDTO> misCitas(String username) {
        Usuario paciente = userRepo.findByUsername(username)
                .orElseThrow(() -> new ReglasDeNegocioException("Usuario autenticado no encontrado."));
        return obtenerCitasPorPaciente(paciente.getId());
    }
    // FB PASO 15 - CU-04 FINAL

    @Override
    @Transactional(readOnly = true)
    public List<CitaDTO> obtenerCitasPorPaciente(Long pacienteId) {
        return citaRepo.findByPacienteIdOrderByFechaHoraDesc(pacienteId).stream()
                .map(mapper::convertirADTO)
                .collect(Collectors.toList());
    }

    // FA03 - CU-03 INICIO
    @Transactional(readOnly = true)
    public CitaDTO obtenerPorId(Long id) {
        return citaRepo.findById(id)
                .map(mapper::convertirADTO)
                .orElseThrow(() -> new ReglasDeNegocioException("La cita solicitada no existe."));
    }
    // FA03 - CU-03 FINAL

    // FA07 - CU-05 INICIO
    // ============================================================
    //  CU-05 FA07: REASIGNACIÓN DE MÉDICO
    // ============================================================

    @Transactional(readOnly = true)
    public ReasignacionDTO prepararReasignacion(Long citaId) {
        Cita cita = citaReasignable(citaId);
        List<OpcionDTO> medicos = userRepo
                .findMedicoBySucursalYEspecialidad(cita.getSucursal().getId(), cita.getEspecialidad().getId())
                .stream()
                .filter(m -> !m.getId().equals(cita.getMedico().getId()))
                .filter(m -> !horarioOcupado(m.getId(), cita.getFechaHora()))
                .map(m -> new OpcionDTO(m.getId(), m.getNombre() + " " + m.getApellidos()))
                .collect(Collectors.toList());
        return new ReasignacionDTO(mapper.convertirADTO(cita), medicos);
    }

    @Transactional
    public CitaDTO reasignarMedico(Long citaId, Long medicoId, String nota, String recepcionista) {
        Cita cita = citaReasignable(citaId);
        if (medicoId == null) {
            throw new ReglasDeNegocioException("Debe seleccionar el nuevo médico.");
        }
        String motivo = nota == null ? "" : nota.trim();
        if (motivo.length() > 300) {
            throw new ReglasDeNegocioException("La nota no puede exceder los 300 caracteres.");
        }
        if (medicoId.equals(cita.getMedico().getId())) {
            throw new ReglasDeNegocioException("Seleccione un médico distinto al actual.");
        }
        Usuario nuevo = userRepo.findByIdParaAgendar(medicoId)
                .orElseThrow(() -> new ReglasDeNegocioException("El médico seleccionado no existe."));
        validarMedico(nuevo, cita.getSucursal().getId(), cita.getEspecialidad().getId());
        if (horarioOcupado(nuevo.getId(), cita.getFechaHora())) {
            throw new ReglasDeNegocioException("El médico seleccionado ya tiene una cita en ese horario.");
        }

        String anterior = cita.getMedico().getNombre() + " " + cita.getMedico().getApellidos();
        cita.setMedico(nuevo);
        citaRepo.saveAndFlush(cita);
        auditoria.registrar("MEDICO_REASIGNADO", "Cita", cita.getId(),
                "De " + anterior + " a " + nuevo.getNombre() + " " + nuevo.getApellidos()
                        + " por " + recepcionista + (motivo.isEmpty() ? "" : ". Nota: " + motivo));
        return mapper.convertirADTO(cita);
    }

    private Cita citaReasignable(Long citaId) {
        Cita cita = citaRepo.findById(citaId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la cita."));
        if (cita.getEstado() != AppointmentStatus.PAGADA && cita.getEstado() != AppointmentStatus.PACIENTE_PRESENTE) {
            throw new ReglasDeNegocioException(
                    "Solo se puede reasignar el médico de citas Confirmadas o con Paciente Presente.");
        }
        return cita;
    }
    // FA07 - CU-05 FINAL

    // El médico debe estar activo y atender esa especialidad en esa sede (agendar, Walk-in y reasignar)
    private void validarMedico(Usuario medico, Long sucursalId, Long especialidadId) {
        boolean atiendeAhi = medico.getRol() == UserRol.MEDICO && Boolean.TRUE.equals(medico.getActivo())
                && medico.getSucursal() != null && medico.getSucursal().getId().equals(sucursalId)
                && medico.getEspecialidad() != null && medico.getEspecialidad().getId().equals(especialidadId);
        if (!atiendeAhi) {
            throw new ReglasDeNegocioException("El médico seleccionado no atiende esa especialidad en esa sede.");
        }
    }

    private boolean horarioOcupado(Long medicoId, LocalDateTime fechaHora) {
        return citaRepo.findHorariosOcupados(medicoId, fechaHora.toLocalDate()).contains(fechaHora)
                || bloqueadoPorEvento(eventosDelDia(medicoId, fechaHora.toLocalDate()), fechaHora);
    }

    private List<EventoAgenda> eventosDelDia(Long medicoId, LocalDate fecha) {
        return eventoRepo.findByMedicoIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(
                medicoId, fecha.plusDays(1).atStartOfDay(), fecha.atStartOfDay());
    }

    // Un espacio [hora, hora + 30 min) está bloqueado si se cruza con algún evento [inicio, fin)
    private boolean bloqueadoPorEvento(List<EventoAgenda> eventos, LocalDateTime espacio) {
        LocalDateTime finEspacio = espacio.plusMinutes(MINUTOS_POR_CITA);
        return eventos.stream().anyMatch(e -> e.getInicio().isBefore(finEspacio) && e.getFin().isAfter(espacio));
    }

    // FA01 - CU-05 INICIO
    // ============================================================
    //  CU-05 FA01: PACIENTE QUE LLEGA POR EMERGENCIA (SIN CITA)
    // ============================================================

    @Transactional(readOnly = true)
    public OpcionDTO sedeDe(String username) {
        return userRepo.findByUsername(username)
                .map(Usuario::getSucursal)
                .map(s -> new OpcionDTO(s.getId(), s.getNombre()))
                .orElseThrow(() -> new ReglasDeNegocioException("Su usuario no tiene una sucursal asignada."));
    }

    @Transactional
    public CitaDTO registrarEmergencia(EmergenciaFormDTO form, String recepcionista) {
        Sucursal sede = userRepo.findByUsername(recepcionista)
                .map(Usuario::getSucursal)
                .orElseThrow(() -> new ReglasDeNegocioException("Su usuario no tiene una sucursal asignada."));

        String dpi = ValidacionDpi.validar(form.getDpi());
        if (form.getEspecialidadId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una especialidad médica para continuar.");
        }
        if (form.getMedicoId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar un médico para continuar.");
        }
        validarMotivo(form.getMotivo());

        Usuario medico = userRepo.findByIdParaAgendar(form.getMedicoId())
                .orElseThrow(() -> new ReglasDeNegocioException("El médico seleccionado no existe."));
        validarMedico(medico, sede.getId(), form.getEspecialidadId());

        Usuario paciente = userRepo.findByDpi(dpi)
                .map(u -> {
                    if (u.getRol() != UserRol.PACIENTE) {
                        throw new ReglasDeNegocioException(
                                "Este DPI pertenece a un usuario del sistema interno. Verifique el DPI del paciente.");
                    }
                    return u;
                })
                .orElseGet(() -> crearPacienteDeEmergencia(dpi, form.getNombre(), form.getApellidos()));

        LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);
        Cita cita = citaRepo.save(Cita.builder()
                .numeroCita(generarNumeroCita())
                .paciente(paciente)
                .medico(medico)
                .sucursal(sede)
                .especialidad(medico.getEspecialidad())
                .fechaHora(ahora)
                .motivoConsulta(form.getMotivo().trim())
                .estado(AppointmentStatus.PACIENTE_PRESENTE)   // pasa directo a toma de signos (CU-07)
                .prioridad(Prioridad.EMERGENCIA)
                .horaLlegada(LocalDateTime.now())
                .build());

        auditoria.registrar("EMERGENCIA_SIN_CITA", "Cita", cita.getId(),
                "Emergencia registrada en recepción por " + recepcionista + " para " + paciente.getNombre()
                        + " " + paciente.getApellidos());
        return mapper.convertirADTO(cita);
    }

    // Registro mínimo con los datos básicos (nombre y DPI); el paciente completa su cuenta después
    private Usuario crearPacienteDeEmergencia(String dpi, String nombre, String apellidos) {
        if (nombre == null || nombre.isBlank() || apellidos == null || apellidos.isBlank()) {
            throw new ReglasDeNegocioException(
                    "El paciente no está registrado: ingrese su nombre y apellidos para registrarlo como emergencia.");
        }
        return userRepo.save(Usuario.builder()
                .nombre(nombre.trim())
                .apellidos(apellidos.trim())
                .dpi(dpi)
                .username("emg" + dpi)
                .email(dpi + "@pendiente.hospital")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .rol(UserRol.PACIENTE)
                .activo(true)
                .intentosFallidos(0)
                .build());
    }
    // FA01 - CU-05 FINAL
}
