package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SedeEspecialidadFilaDTO {
    private Long id;
    private String nombreSucursal;
    private String nombreEspecialidad;
    private boolean activo;
}
