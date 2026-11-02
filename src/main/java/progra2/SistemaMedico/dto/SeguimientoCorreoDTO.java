package progra2.SistemaMedico.dto;

import java.time.LocalDateTime;

public record SeguimientoCorreoDTO(
        Long citaId,
        String numeroCita,
        String nombrePaciente,
        String email,
        LocalDateTime fechaHora,
        String tipo,
        String nombreMedico,
        String nombreEspecialidad,
        String nombreSucursal,
        String observaciones,
        boolean pendientePago) {
}
