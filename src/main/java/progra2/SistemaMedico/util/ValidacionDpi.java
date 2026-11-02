package progra2.SistemaMedico.util;

import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;

// RN-GLOBAL-001: aplica a las búsquedas por DPI de recepción (CU-05), caja (CU-06) y cobro de laboratorio (CU-10)
public final class ValidacionDpi {

    private ValidacionDpi() {
    }

    public static String validar(String dpi) {
        String valor = dpi == null ? "" : dpi.trim();
        if (valor.isEmpty()) {
            throw new ReglasDeNegocioException("El campo DPI es obligatorio. Por favor, ingrese su número de DPI.");
        }
        if (!valor.matches("\\d+")) {
            throw new ReglasDeNegocioException(
                    "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.");
        }
        if (valor.length() != 13) {
            throw new ReglasDeNegocioException(
                    "El DPI debe contener exactamente 13 dígitos. Usted ingresó " + valor.length() + " dígitos.");
        }
        return valor;
    }
}
