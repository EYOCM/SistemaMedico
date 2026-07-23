package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private Long id;

    @NotBlank(message = "El DPI es obligatorio")
    @Pattern(regexp = "^\\d{1,20}$", message = "El DPI debe contener solo números y máximo 13 dígitos")
    private String dpi;

    @Pattern(regexp = "^\\d{1,9}$", message = "El NIT debe tener solo numeros y maximo 9 dijitos")
    private String nit;

    @NotBlank(message = "Ingrese su nombre completo")
    @Size(max = 100, message = "El nombre completo no puede exceder los 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El correo electronico debe ser valido")
    @Size(max = 100, message = "El correo no puede exceder los 100 caracteres")
    private String correoElectronico;

    @Pattern(regexp = "^\\d{0,8}$", message = "El telefono debe contener exactamente 8 dijitos")
    private String telefono;

    @Size(min = 8, max = 12, message = "La contrasenia debe contener al menos 8 y maximo 12 caracteres")
    private String password;

    private Boolean esActivo;
    private Boolean esAdministrador;

}
