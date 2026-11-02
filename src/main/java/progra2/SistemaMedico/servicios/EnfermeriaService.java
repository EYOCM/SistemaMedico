package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.PanelEnfermeriaDTO;
import progra2.SistemaMedico.dto.SignosVitalesDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.SignosVitales;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.Prioridad;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.repositorio.SignosVitalesRepositorio;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnfermeriaService {
    private final CitaRepositorio citaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final CitaMapper citaMapper;
    private final AuditoriaService auditoriaService;
    private final SignosVitalesRepositorio signosVitalesRepositorio;

    private static final BigDecimal TEMP_MIN = new BigDecimal("36.0");
    private static final BigDecimal TEMP_MAX = new BigDecimal("37.5");


    // FB PASO 1 - CU-07 INICIO
    @Transactional(readOnly = true)
    public PanelEnfermeriaDTO obtenerPanel(String username) {
        Usuario enfermera = usuarioRepositorio.findByUsername(username)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el usuario."));
        if (enfermera.getSucursal() == null) {
            throw new ReglasDeNegocioException(
                    "Su usuario no tiene una sede asignada. Contacte al administrador.");
        }
        Long sedeId = enfermera.getSucursal().getId();
        LocalDateTime desde = LocalDate.now().atStartOfDay();
        LocalDateTime hasta = LocalDate.now().atTime(LocalTime.MAX);

        List<CitaDTO> enSala = citaRepositorio
                .findBySucursalIdAndEstadoAndFechaHoraBetweenOrderByHoraLlegadaAsc(
                        sedeId, AppointmentStatus.PACIENTE_PRESENTE, desde, hasta)
                .stream()
                .sorted(Comparator.comparing((Cita c) -> c.getPrioridad() != Prioridad.EMERGENCIA))
                .map(citaMapper::convertirADTO)
                .toList();

        List<CitaDTO> enProceso = citaRepositorio
                .findBySucursalIdAndEstadoAndFechaHoraBetweenOrderByHoraLlegadaAsc(
                        sedeId, AppointmentStatus.SIGNOS_VITALES, desde, hasta)
                .stream()
                .map(citaMapper::convertirADTO)
                .toList();

        return PanelEnfermeriaDTO.builder()
                .nombreSede(enfermera.getSucursal().getNombre())
                .enSala(enSala)
                .enProceso(enProceso)
                .build();
    }
    // FB PASO 1 - CU-07 FINAL

    // FB PASO 1 - CU-07 INICIO
    @Transactional
    public CitaDTO llamar(Long citaId) {
        Cita cita = citaRepositorio.findById(citaId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la cita."));

        if (cita.getEstado() == AppointmentStatus.SIGNOS_VITALES) {
            throw new ReglasDeNegocioException("Este paciente ya fue llamado por otra enfermera.");
        }
        if (cita.getEstado() != AppointmentStatus.PACIENTE_PRESENTE) {
            throw new ReglasDeNegocioException(
                    "Operación no permitida: el paciente no está en sala de espera.");
        }

        cita.setEstado(AppointmentStatus.SIGNOS_VITALES);
        citaRepositorio.saveAndFlush(cita);

        auditoriaService.registrar("LLAMADO_SIGNOS", "Cita", cita.getId(),
                "Paciente llamado a toma de signos vitales");

        return citaMapper.convertirADTO(cita);
    }
    // FB PASO 1 - CU-07 FINAL

    //CU7
    // FB PASOS 2-3 - CU-07 INICIO
    @Transactional(readOnly = true)
    public CitaDTO obtenerCitaParaSignos(Long citaId) {
        return citaMapper.convertirADTO(buscarEnToma(citaId));
    }
    // FB PASOS 2-3 - CU-07 FINAL

    // FB PASOS 10-11 - CU-07 INICIO
    @Transactional
    public CitaDTO registrar(Long citaId, SignosVitalesDTO datos, String enfermera) {
        Cita cita = buscarEnToma(citaId);

        // FA03 - CU-07 INICIO
        List<String> lista = calcularAlertas(datos);
        String alertas = lista.isEmpty() ? null : String.join(" ", lista);
        // FA03 - CU-07 FINAL

        // FA01 - CU-07 INICIO
        if (datos.isEmergencia()) {
            cita.setPrioridad(Prioridad.EMERGENCIA);
        }
        // FA01 - CU-07 FINAL
        cita.setEstado(AppointmentStatus.EN_ESPERA);
        citaRepositorio.saveAndFlush(cita);

        signosVitalesRepositorio.save(SignosVitales.builder()
                .cita(cita)
                .sistolica(datos.getSistolica())
                .diastolica(datos.getDiastolica())
                .temperatura(datos.getTemperatura())
                .peso(datos.getPeso())
                .talla(datos.getTalla())
                .frecuenciaCardiaca(datos.getFrecuenciaCardiaca())
                .alertas(alertas)
                .registradoPor(enfermera)
                .fechaRegistro(LocalDateTime.now())
                .build());

        auditoriaService.registrar("SIGNOS_VITALES", "Cita", cita.getId(),
                "Signos registrados." + (alertas != null ? " " + alertas : ""));

        return citaMapper.convertirADTO(cita);
    }
    // FB PASOS 10-11 - CU-07 FINAL

    private Cita buscarEnToma(Long citaId) {
        Cita cita = citaRepositorio.findById(citaId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la cita."));
        if (cita.getEstado() != AppointmentStatus.SIGNOS_VITALES) {
            throw new ReglasDeNegocioException("La toma de signos de este paciente no está en curso.");
        }
        return cita;
    }

    // FA03 - CU-07 INICIO
    private List<String> calcularAlertas(SignosVitalesDTO d) {
        List<String> alertas = new ArrayList<>();
        if (d.getSistolica() < 90 || d.getSistolica() > 140
                || d.getDiastolica() < 60 || d.getDiastolica() > 90) {
            alertas.add("Presión arterial fuera de rango normal.");
        }
        if (d.getTemperatura().compareTo(TEMP_MIN) < 0 || d.getTemperatura().compareTo(TEMP_MAX) > 0) {
            alertas.add("Temperatura fuera de rango normal.");
        }
        if (d.getFrecuenciaCardiaca() < 60 || d.getFrecuenciaCardiaca() > 100) {
            alertas.add("Frecuencia cardíaca fuera de rango normal.");
        }
        return alertas;
    }
    // FA03 - CU-07 FINAL
}
