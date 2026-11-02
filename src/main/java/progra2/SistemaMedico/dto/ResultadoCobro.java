package progra2.SistemaMedico.dto;

import progra2.SistemaMedico.model.enums.MetodoPago;

import java.math.BigDecimal;

// clase solo para cargar datos Un `record` declara los campos **en una línea** y Java genera el constructor y los "getters".
public record ResultadoCobro(MetodoPago metodo,
                             BigDecimal montoRecibido,
                             BigDecimal cambio,
                             String ultimosDigitos,
                             String numeroTransaccion) {
}
