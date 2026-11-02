package progra2.SistemaMedico.model.enums;

public enum EstadoDespacho {
    COMPLETADO("Despachado"),
    NO_ADQUIRIDO("No adquirido");

    private final String descripcion;

    EstadoDespacho(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
