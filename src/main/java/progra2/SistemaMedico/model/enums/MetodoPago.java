package progra2.SistemaMedico.model.enums;

import java.util.List;

public enum MetodoPago {
    TARJETA_EN_LINEA("Tarjeta (pago en línea)"),
    EFECTIVO("Efectivo"),
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    DEBITO("Tarjeta de débito");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esTarjeta() {
        return this == VISA || this == MASTERCARD || this == DEBITO;
    }

    // FB PASO 4 - CU-06 INICIO
    // FB PASO 6 - CU-10 INICIO
    public static List<MetodoPago> enCaja() {
        return List.of(EFECTIVO, VISA, MASTERCARD, DEBITO);
    }
    // FB PASO 6 - CU-10 FINAL
    // FB PASO 4 - CU-06 FINAL
}
