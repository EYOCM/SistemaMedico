package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import progra2.SistemaMedico.model.enums.EstadoConsulta;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaDTO {

    @NotBlank(message = "El motivo de la consulta es obligatorio.")
    @Size(max = 1000, message = "El motivo no puede exceder 1000 caracteres.")
    private String motivo;

    @Size(max = 5000, message = "Los hallazgos no pueden exceder 5000 caracteres.")
    private String hallazgos;

    @Size(max = 10, message = "Código CIE-10 no válido.")
    private String codigoCie10;

    @Size(max = 5000, message = "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.")
    private String diagnostico;

    @Size(max = 5000, message = "El plan de tratamiento no puede exceder 5000 caracteres.")
    private String planTratamiento;

    @Size(max = 2000, message = "Las notas no pueden exceder 2000 caracteres.")
    private String notas;

    @NotNull(message = "Seleccione el estado de la consulta.")
    private EstadoConsulta estado;
}