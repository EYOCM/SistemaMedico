package progra2.SistemaMedico.dto;

import lombok.Data;
import progra2.SistemaMedico.model.enums.UserRol;


 // objeto que respalda el formulario web de Crear / Editar usuario.

@Data
public class UsuarioFormDTO {

    private Long id;                 // null cuando es creacion

    private String nombre;
    private String apellidos;
    private String username;
    private String password;         // en edicion: vacio = no cambiar
    private String email;
    private String dpi;              // opcional  (RN-CU01-07)
    private String nit;              // opcional  (RN-CU01-11)
    private String telefono;         // opcional  (RN-CU01-08)
    private String numeroSeguro;     // opcional  (RN-CU01-12)

    private UserRol rol;             // obligatorio (RN-CU01-09)
    private Boolean activo;          // obligatorio (RN-CU01-10)

    private Long sucursalId;         // RN-CU01-13
    private Long especialidadId;     // RN-CU01-14, solo para MEDICO
}
