package progra2.SistemaMedico.model.enums;

import java.util.Arrays;
import java.util.List;

public enum TipoMovimiento {
    // FB PASO 4 - CU-15 INICIO
    COMPRA("Compra", true, "Factura", "Notas (opcional)", false, true),
    DEVOLUCION("Devolución", true, "Devolución", "Motivo de Devolución", true, true),
    VENTA("Venta", false, "Venta", null, false, true),
    RECLAMO("Reclamo", false, "Reclamo", "Motivo del Reclamo", true, true),
    AJUSTE_ENTRADA("Ajuste +", true, "Referencia", "Notas/Justificación", true, true),
    AJUSTE_SALIDA("Ajuste -", false, "Referencia", "Notas/Justificación", true, true),
    DESPACHO("Despacho", false, "Despacho", "Receta", false, false);
    // FB PASO 4 - CU-15 FINAL

    private final String descripcion;
    private final boolean entrada;
    private final String etiquetaReferencia;
    private final String etiquetaMotivo;
    private final boolean motivoObligatorio;
    private final boolean manual;

    TipoMovimiento(String descripcion, boolean entrada, String etiquetaReferencia,
                   String etiquetaMotivo, boolean motivoObligatorio, boolean manual) {
        this.descripcion = descripcion;
        this.entrada = entrada;
        this.etiquetaReferencia = etiquetaReferencia;
        this.etiquetaMotivo = etiquetaMotivo;
        this.motivoObligatorio = motivoObligatorio;
        this.manual = manual;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isEntrada() {
        return entrada;
    }

    public String getEtiquetaReferencia() {
        return etiquetaReferencia;
    }

    public String getEtiquetaMotivo() {
        return etiquetaMotivo;
    }

    public boolean isMotivoObligatorio() {
        return motivoObligatorio;
    }

    public boolean isManual() {
        return manual;
    }

    // FB PASO 4 - CU-15 INICIO
    public static List<TipoMovimiento> manuales() {
        return Arrays.stream(values()).filter(TipoMovimiento::isManual).toList();
    }
    // FB PASO 4 - CU-15 FINAL
}
