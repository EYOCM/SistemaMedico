package progra2.SistemaMedico.model.enums;

public enum TipoEvento {
    // FB PASO 4 - CU-16 INICIO
    REUNION("Reunión"),
    DESCANSO("Descanso"),
    CAPACITACION("Capacitación"),
    PERSONAL("Personal"),
    OTRO("Otro");
    // FB PASO 4 - CU-16 FINAL

    private final String descripcion;

    TipoEvento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
