package progra2.SistemaMedico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 objeto que respalda el formulario publico de registro de pacientes.
 *
 * Toda la validacion de FORMATO vive aqui, en anotaciones de Bean Validation,
**/

@Data
public class RegistroDTO {

    // FB PASOS 2-9 - CU-02 INICIO
    // Nombre completo la longitud combinada
    @NotBlank(message = "El campo Nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El campo Apellidos es obligatorio.")
    @Size(max = 100, message = "Los apellidos no pueden exceder los 100 caracteres.")
    private String apellidos;

    //  DPI
    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "^\\d*$",
             message = "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.")
    @Size(min = 13, max = 13, message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    //  NIT (obligatorio en el registro externo)
    @NotBlank(message = "El campo NIT es obligatorio.")
    @Pattern(regexp = "^[a-zA-Z0-9]*$",
             message = "El NIT debe contener únicamente caracteres alfanuméricos.")
    @Size(min = 8, max = 9, message = "El NIT debe contener entre 8 y 9 caracteres.")
    private String nit;

    // Telefon
    @NotBlank(message = "El campo Teléfono es obligatorio.")
    @Pattern(regexp = "^\\d{8}$",
             message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    private String telefono;

    // seguro opcional
    @Pattern(regexp = "^$|^.{5,50}$",
             message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro;

    //  Correo electronico
    @NotBlank(message = "El campo Correo Electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com")
    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres.")
    private String email;

    // RN-CU02-05: Nombre de usuario
    @NotBlank(message = "El campo Usuario es obligatorio.")
    @Pattern(regexp = "^[a-zA-Z0-9]*$",
             message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    @Size(min = 8, max = 9, message = "El usuario debe contener entre 8 y 9 caracteres.")
    private String username;

    // Contrasena
    @NotBlank(message = "El campo Contraseña es obligatorio.")
    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;
    // FB PASOS 2-9 - CU-02 FINAL
}
