package progra2.SistemaMedico.model.enums;

public enum EstadoOrdenLaboratorio {
    PENDIENTE("Pendiente de pago"),
    EN_PROCESO("En proceso"),
    COMPLETADA("Completada");

    private final String descripcion;

    EstadoOrdenLaboratorio(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
