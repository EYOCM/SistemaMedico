package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaResumenDTO {
    private String numeroReceta;
    private LocalDateTime fechaEmision;
    private String estado;
    private List<RecetaFilaDTO> filas;
}
