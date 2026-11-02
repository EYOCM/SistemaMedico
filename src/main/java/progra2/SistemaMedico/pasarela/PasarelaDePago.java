package progra2.SistemaMedico.pasarela;

import progra2.SistemaMedico.dto.PagoDTO;
import progra2.SistemaMedico.model.enums.MetodoPago;

import java.math.BigDecimal;

public interface PasarelaDePago {
    RespuestaPasarela cobrar(BigDecimal monto, PagoDTO datosTarjeta, String claveIdempotencia);

    RespuestaPasarela cobrarEnTerminal(BigDecimal monto, MetodoPago tipo, String ultimosDigitos);
}
