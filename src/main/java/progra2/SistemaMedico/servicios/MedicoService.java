package progra2.SistemaMedico.servicios;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.ConsultaDTO;
import progra2.SistemaMedico.dto.FormularioConsultaDTO;
import progra2.SistemaMedico.dto.PanelMedicoDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cie10;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Consulta;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.EstadoConsulta;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.repositorio.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MedicoService {
    private final CitaRepositorio citaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final CitaMapper mapper;
    private final AuditoriaService auditoriaService;
    private final ConsultaRepositorio consultaRepositorio;
    private final Cie10Repositorio cie10Repositorio;
    private final SignosVitalesRepositorio signosVitalesRepositorio;
    private final RecetaRepositorio recetaRepositorio;
    private final OrdenLaboratorioRepositorio ordenLaboratorioRepositorio;

    // FB PASO 1 - CU-08 INICIO
    @Transactional(readOnly = true)
    public PanelMedicoDTO obtenerPanel(String username) {
        Usuario medico = usuarioRepositorio.findByUsername(username).
                orElseThrow(() -> new ReglasDeNegocioException("No se encontró el usuario."));

        /*
         * ¿Por qué != y no ==? Si usáramos == EMERGENCIA, Elena valdría true y se iría al final,
         * porque true va después de false. Con != la emergencia vale false y sube. Es un truco
         *  para que lo importante quede arriba.*/

        List<CitaDTO> enEspera = citasDeHoy(username, AppointmentStatus.EN_ESPERA).stream()
                //.sorted = ordenar sorted necesita saber con qué criterio ordenar. Ese criterio se llama Comparator
                .sorted(Comparator.comparing((Cita c) -> c.getPrioridad() != Prioridad.EMERGENCIA))
                .map(mapper::convertirADTO) // :: referencia a metodo
                .toList();

        List<CitaDTO> enConsulta = citasDeHoy(username, AppointmentStatus.CONSULTA_MEDICA).stream()
                .map(mapper::convertirADTO)
                .toList();

        List<CitaDTO> evaluados = citasDeHoy(username, AppointmentStatus.EVALUADO).stream()
                .map(mapper::convertirADTO)
                .toList();

        Set<Long> citasConReceta = evaluados.stream()
                .map(CitaDTO::getId)
                .filter(recetaRepositorio::existsByCitaId)
                .collect(Collectors.toSet());

        Set<Long> citasConOrden = evaluados.stream()
                .map(CitaDTO::getId)
                .filter(ordenLaboratorioRepositorio::existsByCitaId)
                .collect(Collectors.toSet());

        return PanelMedicoDTO.builder()
                .nombreMedico("Dr(a). " + medico.getNombre() + " " + medico.getApellidos())
                .enEspera(enEspera)
                .enConsulta(enConsulta)
                .evaluados(evaluados)
                .citasConReceta(citasConReceta)
                .citasConOrden(citasConOrden)
                .build();
    }
    // FB PASO 1 - CU-08 FINAL

    // FB PASO 2 - CU-08 INICIO
    //inicio de consulta y cambio de estado de cita
    @Transactional
    public CitaDTO iniciar(Long citaId, String username) {
        Cita cita = buscarPropia(citaId, username);
        if (cita.getEstado() != AppointmentStatus.EN_ESPERA) {
            throw new ReglasDeNegocioException("Operación no permitida: el paciente no está en espera de consulta.");
        }
        cita.setEstado(AppointmentStatus.CONSULTA_MEDICA);
        citaRepositorio.saveAndFlush(cita); //Manda el `UPDATE` a PostgreSQL **ya**, no al final del método
        auditoriaService.registrar("CONSULTA_INICIADA", "Cita", cita.getId(), "Consulta médica iniciada");
        return mapper.convertirADTO(cita);
    }
    // FB PASO 2 - CU-08 FINAL

    // FA06 - CU-08 INICIO
    @Transactional
    public CitaDTO marcarNoAsistio(Long citaId, String username) {
        Cita cita = buscarPropia(citaId, username);
        if (cita.getEstado() != AppointmentStatus.EN_ESPERA) {
            throw new ReglasDeNegocioException("Solo se puede marcar No Asistió a un paciente en espera de consulta.");
        }
        cita.setEstado(AppointmentStatus.NO_ASISTIO);
        citaRepositorio.saveAndFlush(cita);//Manda el `UPDATE` a PostgreSQL **ya**, no al final del método
        auditoriaService.registrar("NO_ASISTIO", "Cita", cita.getId(), "Paciente no asistió a consulta");
        return mapper.convertirADTO(cita);
    }
    // FA06 - CU-08 FINAL


    // FB PASOS 11-12 - CU-08 INICIO
    // finalizar una consulta con estado evaluado
    @Transactional
    public CitaDTO finalizarAtencion(Long citaId, String username) {
        Cita cita = buscarPropia(citaId, username);
        if (cita.getEstado() != AppointmentStatus.EVALUADO) {
            throw new ReglasDeNegocioException("Solo se puede finalizar la atención de una consulta evaluada.");
        }
        cita.setEstado(AppointmentStatus.ATENCION_FINALIZADA);
        citaRepositorio.saveAndFlush(cita); //Manda el `UPDATE` a PostgreSQL **ya**, no al final del método
        auditoriaService.registrar("ATENCION_FINALIZADA", "Cita", cita.getId(), "Atención finalizada");
        return mapper.convertirADTO(cita);
    }
    // FB PASOS 11-12 - CU-08 FINAL


    // FB PASOS 3-4 - CU-08 INICIO
    @Transactional(readOnly = true)
    public FormularioConsultaDTO prepararFormulario(Long citaId, String username) {
        Cita cita = buscarEnConsulta(citaId, username);
        Optional<Consulta> guardada = consultaRepositorio.findByCitaId(citaId);

        ConsultaDTO consulta = guardada
                .map(c -> ConsultaDTO.builder()
                        .motivo(c.getMotivo())
                        .hallazgos(c.getHallazgos())
                        .codigoCie10(c.getCodigoCie10())
                        .diagnostico(c.getDiagnostico())
                        .planTratamiento(c.getPlanTratamiento())
                        .notas(c.getNotas())
                        .estado(c.getEstado())
                        .build())
                .orElseGet(() -> ConsultaDTO.builder()
                        .motivo(cita.getMotivoConsulta())
                        .estado(EstadoConsulta.EN_CURSO)
                        .build());

        return FormularioConsultaDTO.builder()
                .cita(mapper.convertirADTO(cita))
                .consulta(consulta)
                .historialSignos(signosVitalesRepositorio
                        .findByCitaPacienteIdOrderByFechaRegistroDesc(cita.getPaciente().getId()))
                .consultaGuardada(guardada.isPresent())
                .seguimiento(guardada
                        .flatMap(c -> citaRepositorio.findFirstByConsultaOrigenIdAndEstadoNot(c.getId(), AppointmentStatus.CANCELADA))
                        .map(mapper::convertirADTO)
                        .orElse(null))
                .build();
    }
    // FB PASOS 3-4 - CU-08 FINAL

    // FB PASOS 5-10 - CU-08 INICIO
    @Transactional
    public EstadoConsulta guardarConsulta(Long citaId, ConsultaDTO datos, String username) {
        Cita cita = buscarEnConsulta(citaId, username);
        boolean finalizar = datos.getEstado() == EstadoConsulta.FINALIZADA;

        // FA05 - CU-08 INICIO
        if (finalizar) {
            if (vacio(datos.getDiagnostico())) {
                throw new ReglasDeNegocioException(
                        "No es posible finalizar la consulta sin registrar un diagnóstico. El campo Diagnóstico es obligatorio.");
            }
            if (datos.getDiagnostico().trim().length() < 10) {
                throw new ReglasDeNegocioException(
                        "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.");
            }
            if (vacio(datos.getHallazgos()) || vacio(datos.getPlanTratamiento())) {
                throw new ReglasDeNegocioException(
                        "Debe completar todos los campos obligatorios para cerrar la consulta.");
            }
        }
        // FA05 - CU-08 FINAL

        // FB PASO 7 - CU-08 INICIO
        String codigo = vacio(datos.getCodigoCie10()) ? null : datos.getCodigoCie10().trim().toUpperCase();
        if (codigo != null && !cie10Repositorio.existsById(codigo)) {
            throw new ReglasDeNegocioException("El código CIE-10 seleccionado no existe en el catálogo.");
        }
        // FB PASO 7 - CU-08 FINAL

        LocalDateTime ahora = LocalDateTime.now();
        Consulta consulta = consultaRepositorio.findByCitaId(citaId)
                .orElseGet(() -> Consulta.builder().cita(cita).fechaInicio(ahora).build());

        consulta.setMotivo(datos.getMotivo().trim());
        consulta.setHallazgos(datos.getHallazgos());
        consulta.setCodigoCie10(codigo);
        consulta.setDiagnostico(datos.getDiagnostico());
        consulta.setPlanTratamiento(datos.getPlanTratamiento());
        consulta.setNotas(datos.getNotas());
        consulta.setEstado(datos.getEstado());

        // FB PASO 10 - CU-08 INICIO
        if (finalizar) {
            consulta.setFechaFinalizacion(ahora);
            cita.setEstado(AppointmentStatus.EVALUADO);
            citaRepositorio.saveAndFlush(cita);
        }
        // FB PASO 10 - CU-08 FINAL
        consultaRepositorio.saveAndFlush(consulta);

        auditoriaService.registrar(finalizar ? "CONSULTA_FINALIZADA" : "CONSULTA_GUARDADA",
                "Cita", cita.getId(), "CIE-10: " + (codigo != null ? codigo : "—"));

        return consulta.getEstado();
    }
    // FB PASOS 5-10 - CU-08 FINAL

    // FB PASO 7 - CU-08 INICIO
    @Transactional(readOnly = true)
    public List<Cie10> buscarCie10(String texto) {
        if (texto == null || texto.trim().length() < 2) {
            return List.of();
        }
        String t = texto.trim();
        return cie10Repositorio
                .findTop10ByCodigoContainingIgnoreCaseOrDescripcionContainingIgnoreCaseOrderByCodigoAsc(t, t);
    }
    // FB PASO 7 - CU-08 FINAL

    // FB PASO 11 - CU-08 INICIO
    public Cita buscarEvaluada(Long citaId, String username) {
        Cita cita = buscarPropia(citaId, username);
        if (cita.getEstado() != AppointmentStatus.EVALUADO) {
            throw new ReglasDeNegocioException(
                    "Solo se pueden generar indicaciones para una consulta evaluada.");
        }
        return cita;
    }
    // FB PASO 11 - CU-08 FINAL

    public Cita buscarEnConsulta(Long citaId, String username) {
        Cita cita = buscarPropia(citaId, username);
        if (cita.getEstado() != AppointmentStatus.CONSULTA_MEDICA) {
            throw new ReglasDeNegocioException("La consulta de este paciente no está en curso.");
        }
        return cita;
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private Cita buscarPropia(Long citaId, String username) {
        Cita cita = citaRepositorio.findById(citaId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la cita."));
        if (!cita.getMedico().getUsername().equals(username)) {
            throw new ReglasDeNegocioException("Esta cita no está asignada a usted.");
        }
        return cita;
    }

    private List<Cita> citasDeHoy(String username, AppointmentStatus estado) {
        return citaRepositorio.findByMedicoUsernameAndEstadoAndFechaHoraBetweenOrderByHoraLlegadaAsc(
                username, estado, LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX));

    }

}
