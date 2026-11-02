package progra2.SistemaMedico.util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// FA02 - CU-09 INICIO
public final class RangoReferencia {

    private static final Pattern ENTRE = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*-\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern LIMITE = Pattern.compile("^\\s*([<>])\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");

    private RangoReferencia() {
    }

    public static Optional<Boolean> estaFuera(String valor, String rango) {
        if (valor == null || rango == null) {
            return Optional.empty();
        }
        BigDecimal numero;
        try {
            numero = new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        Matcher entre = ENTRE.matcher(rango);
        if (entre.matches()) {
            BigDecimal minimo = new BigDecimal(entre.group(1));
            BigDecimal maximo = new BigDecimal(entre.group(2));
            return Optional.of(numero.compareTo(minimo) < 0 || numero.compareTo(maximo) > 0);
        }

        Matcher limite = LIMITE.matcher(rango);
        if (limite.matches()) {
            BigDecimal tope = new BigDecimal(limite.group(2));
            boolean menorQue = limite.group(1).equals("<");
            return Optional.of(menorQue ? numero.compareTo(tope) >= 0 : numero.compareTo(tope) <= 0);
        }
        return Optional.empty();
    }
}
// FA02 - CU-09 FINAL
