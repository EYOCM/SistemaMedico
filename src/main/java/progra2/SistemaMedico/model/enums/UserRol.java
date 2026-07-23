package progra2.SistemaMedico.model.enums;

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
