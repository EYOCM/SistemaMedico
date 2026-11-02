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
public class SucursalFormDTO {

    private Long id;

    // FA05 - CU-14 INICIO
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
    private String nombre;

    @Pattern(regexp = "^$|^\\d{8}$", message = "El teléfono debe tener exactamente 8 dígitos.")
    private String telefono;

    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres.")
    private String direccion;

    @Size(max = 250, message = "La descripción no puede exceder 250 caracteres.")
    private String descripcion;

    @Email(message = "El correo electrónico no es válido.")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres.")
    private String email;

    @NotNull(message = "El estado es obligatorio.")
    @Builder.Default
    private Boolean activo = true;
    // FA05 - CU-14 FINAL
}
