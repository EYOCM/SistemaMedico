package progra2.SistemaMedico.dto;

import java.util.List;

public record ReasignacionDTO(CitaDTO cita, List<OpcionDTO> medicos) {
}
