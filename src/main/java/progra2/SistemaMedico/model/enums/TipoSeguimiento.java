package progra2.SistemaMedico.model.enums;

public enum TipoSeguimiento {
    // FB PASO 3 - CU-12 INICIO
    MONITOREO_TRATAMIENTO("Monitoreo de Tratamiento"),
    REVISION_RESULTADOS("Revisión de Resultados de Laboratorio");
    // FB PASO 3 - CU-12 FINAL

    private final String descripcion;

    TipoSeguimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
