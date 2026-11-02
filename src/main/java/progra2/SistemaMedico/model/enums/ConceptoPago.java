package progra2.SistemaMedico.model.enums;

public enum ConceptoPago {
    CONSULTA("Consulta médica"),
    // FB PASO 9 - CU-10 INICIO
    LABORATORIO("Laboratorio");
    // FB PASO 9 - CU-10 FINAL

    private final String descripcion;

    ConceptoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
