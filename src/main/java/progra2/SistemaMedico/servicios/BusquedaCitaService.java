package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.ResultadoBusquedaCitaDTO;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import progra2.SistemaMedico.util.ValidacionDpi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

//servicio compartido por caja y recepcion
@Service
@RequiredArgsConstructor
public class BusquedaCitaService {
    private final CitaRepositorio citaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final CitaMapper mapper;

    // FB PASO 3 - CU-05 INICIO
    @Transactional(readOnly = true)
    public ResultadoBusquedaCitaDTO buscarPorNumero(String numero, Collection<AppointmentStatus> estados) {
        String numeroLimpio = numero.trim().toUpperCase();
        Optional<Cita> encontrada = citaRepositorio.findByNumeroCita(numeroLimpio)
                .filter(cita -> estados.contains(cita.getEstado()) && estaVigente(cita));

        if (encontrada.isEmpty()) {
            return ResultadoBusquedaCitaDTO.builder()
                    .tipo(ResultadoBusquedaCitaDTO.TipoResultado.SIN_RESULTADOS)
                    .build();
        }
        CitaDTO cita = mapper.convertirADTO(encontrada.get());
        return ResultadoBusquedaCitaDTO.builder()
                .tipo(ResultadoBusquedaCitaDTO.TipoResultado.CITAS_ENCONTRADAS)
                .nombrePaciente(cita.getNombrePaciente())
                .citas(List.of(cita))
                .build();
    }
    // FB PASO 3 - CU-05 FINAL

    // FA02 - CU-05 INICIO
    @Transactional(readOnly = true)
    public ResultadoBusquedaCitaDTO buscarPorDpi(String dpi, Collection<AppointmentStatus> estados) {

        String dpiLimpio = ValidacionDpi.validar(dpi);
        Optional<Usuario> encontrado = usuarioRepositorio.findByDpi(dpiLimpio);
        // FA03 - CU-05 INICIO
        if (encontrado.isEmpty() || encontrado.get().getRol() != UserRol.PACIENTE) {
            return ResultadoBusquedaCitaDTO.builder()
                    .tipo(ResultadoBusquedaCitaDTO.TipoResultado.PACIENTE_NO_EXISTE)
                    .dpi(dpiLimpio)
                    .build();
        }
        // FA03 - CU-05 FINAL
        Usuario paciente = encontrado.get();
        String nombre = paciente.getNombre() + " " + paciente.getApellidos();

        List<Cita> citas = citaRepositorio
                .findByPacienteIdAndEstadoInAndFechaHoraGreaterThanEqualOrderByFechaHoraAsc(
                        paciente.getId(), estados, LocalDate.now().atStartOfDay())
                .stream()
                .filter(this::estaVigente)
                .toList();

        // FA04 - CU-05 INICIO
        if (citas.isEmpty()) {
            return ResultadoBusquedaCitaDTO.builder()
                    .tipo(ResultadoBusquedaCitaDTO.TipoResultado.SIN_CITAS_ACTIVAS)
                    .nombrePaciente(nombre)
                    .dpi(dpiLimpio)
                    .build();
        }
        // FA04 - CU-05 FINAL

        List<CitaDTO> citasDTO = citas.stream()
                .map(mapper::convertirADTO)
                .toList();

        return ResultadoBusquedaCitaDTO.builder()
                .tipo(ResultadoBusquedaCitaDTO.TipoResultado.CITAS_ENCONTRADAS)
                .nombrePaciente(nombre)
                .dpi(dpiLimpio)
                .citas(citasDTO)
                .build();
    }
    // FA02 - CU-05 FINAL

    public boolean estaVigente(Cita cita) {
        return cita.getEstado() != AppointmentStatus.PENDIENTE_PAGO
                || cita.getFechaReservaExpira() == null
                || cita.getFechaReservaExpira().isAfter(LocalDateTime.now());
    }

}
