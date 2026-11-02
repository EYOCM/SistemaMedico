package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SedeEspecialidadFormDTO {

    // FA03 - CU-13 INICIO
    @NotNull(message = "Debe seleccionar una sede.")
    private Long sucursalId;

    @NotNull(message = "Debe seleccionar una especialidad.")
    private Long especialidadId;
    // FA03 - CU-13 FINAL
}
