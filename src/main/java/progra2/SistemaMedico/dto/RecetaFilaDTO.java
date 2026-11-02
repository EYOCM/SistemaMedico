package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaFilaDTO {

    private Long medicamentoId;

    private String nombreMedicamento;

    @Size(max = 100, message = "La dosis no puede exceder 100 caracteres.")
    private String dosis;

    @Size(max = 100, message = "La frecuencia no puede exceder 100 caracteres.")
    private String frecuencia;

    @Size(max = 100, message = "La duración no puede exceder 100 caracteres.")
    private String duracion;

    @Size(max = 500, message = "Las indicaciones no pueden exceder 500 caracteres.")
    private String indicaciones;
}
