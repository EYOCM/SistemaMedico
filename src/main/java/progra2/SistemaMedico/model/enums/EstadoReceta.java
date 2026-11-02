package progra2.SistemaMedico.model.enums;

public enum EstadoReceta {
    PENDIENTE("Pendiente de despacho"),
    DESPACHADA("Despachada"),
    NO_ADQUIRIDA("No adquirida");

    private final String descripcion;

    EstadoReceta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
