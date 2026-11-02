package progra2.SistemaMedico.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class LuhnValidator implements ConstraintValidator<Luhn, String> {

    // FA01 - CU-04 INICIO
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {

        if (valor == null || valor.isBlank()) return true;
        if (!valor.matches("\\d+")) return true;
        if (valor.length() < 13 || valor.length() > 19) return true;

        int suma = 0;
        boolean duplicar = false;

        // Recorrer de derecha a izquierda
        for (int i = valor.length() - 1; i >= 0; i--) {
            int digito = valor.charAt(i) - '0';   // '7' - '0' = 7

            if (duplicar) {
                digito = digito * 2;
                if (digito > 9) {
                    digito = digito - 9;          // 16 -> 7, 18 -> 9
                }
            }

            suma += digito;
            duplicar = !duplicar;    // uno sí, uno no
        }

        return suma % 10 == 0;
    }
    // FA01 - CU-04 FINAL
}
