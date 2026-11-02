package progra2.SistemaMedico.model.enums;

public enum PrioridadTarea {
    // FA06 - CU-16 INICIO
    BAJA("Baja", "#198754"),
    NORMAL("Normal", "#0d6efd"),
    ALTA("Alta", "#dc3545");
    // FA06 - CU-16 FINAL

    private final String descripcion;
    private final String color;

    PrioridadTarea(String descripcion, String color) {
        this.descripcion = descripcion;
        this.color = color;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getColor() {
        return color;
    }
}
