package progra2.SistemaMedico.model.enums;

/*
 * CU-14: los "Roles" se muestran en /admin/catalogos solo para consulta y NO se editan.
 * SecurityConfig decide qué pantallas abre cada rol por su nombre (hasRole("MEDICO"), hasRole("CAJERO"), ...)
 * y VistaController redirige al panel de cada rol al iniciar sesión.
 * Un rol nuevo o renombrado desde la base no tendría pantallas ni permisos, por eso viven en el código.
 */
public enum UserRol {
    /* enumeracion de roles disponinles del sistema,  Según RN-CU01-03*/
    MEDICO("Médico"),
    ENFERMERO("Enfermero"),
    RECEPCIONISTA("Recepcionista"),
    CAJERO("Cajero"),
    LABORATORISTA("Laboratorista"),
    FARMACEUTICO("Farmacéutico"),
    ADMINISTRADOR("Administrador"),
    PACIENTE("Paciente");

    private final String descripcion;

    UserRol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
