package progra2.SistemaMedico.model.enums;

public enum Prioridad {
    NORMAL("Normal"),
    EMERGENCIA("Emergencia");


    private final String descripcion;

    Prioridad (String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

}
