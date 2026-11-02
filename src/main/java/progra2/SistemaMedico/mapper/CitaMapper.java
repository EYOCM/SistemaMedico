package progra2.SistemaMedico.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.model.Cita;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class CitaMapper {


    public CitaDTO convertirADTO(Cita cita) {
        if (cita == null) return null;
// calculo de segundos
        long segundos = cita.getFechaReservaExpira() == null
                ? 0 //Si la reserva ya venció, between da un número negativo
                : ChronoUnit.SECONDS.between(LocalDateTime.now(), cita.getFechaReservaExpira()); // calcula cuanto tiempo falta entre la fecha de expiracion y ahora

        return CitaDTO.builder()
                .id(cita.getId())
                .numeroCita(cita.getNumeroCita())
                .nombreSucursal(cita.getSucursal().getNombre())
                .nombreEspecialidad(cita.getEspecialidad().getNombre())
                // obtenemos el nombre y apellido del medico
                .nombreMedico(cita.getMedico().getNombre() + " " + cita.getMedico().getApellidos())
                .fechaHora(cita.getFechaHora())
                .motivo(cita.getMotivoConsulta())
                .estadoDescripcion(cita.getEstado().getDescripcion())
                .estado(cita.getEstado().name())// conversion del enum a String con el .name
                .monto(cita.getEspecialidad().getPrecioConsulta())
                .numeroTransaccion(cita.getNumeroTransaccion())
                .segundosRestantes(Math.max(0, segundos)) // si segundos es un numero negativo lo convierte en 0 y se cumple el FA02
                .nombrePaciente(cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidos())
                .montoPago(cita.getMontoPago())
                .fechaPago(cita.getFechaPago())
                .horaLlegada(cita.getHoraLlegada())
                .prioridad(cita.getPrioridad() == null ? "NORMAL" : cita.getPrioridad().name())
                .tipoSeguimiento(cita.getTipoSeguimiento() == null ? null : cita.getTipoSeguimiento().getDescripcion())
                .reservaSinVencimiento(cita.getFechaReservaExpira() == null)
                // para tomar los datos de la sesion vencida y enviar al paso 4 del wizard
                .sucursalId(cita.getSucursal().getId())
                .especialidadId(cita.getEspecialidad().getId())
                .medicoId(cita.getMedico().getId())
                .dpiPaciente(cita.getPaciente().getDpi())
                .build();

    }

}
