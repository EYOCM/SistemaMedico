package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioFormDTO {

    private Long id;

    // FA05 - CU-14 INICIO
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres.")
    private String descripcion;

    @NotNull(message = "El estado es obligatorio.")
    @Builder.Default
    private Boolean activo = true;
    // FA05 - CU-14 FINAL
}
