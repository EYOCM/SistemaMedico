package progra2.SistemaMedico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoBusquedaCitaDTO {

    public enum TipoResultado {
        CITAS_ENCONTRADAS,
        SIN_RESULTADOS,
        PACIENTE_NO_EXISTE,
        SIN_CITAS_ACTIVAS
    }

    private TipoResultado tipo;
    private String nombrePaciente;
    private String dpi;
    private List<CitaDTO> citas;
}
