package progra2.SistemaMedico.pasarela;

import org.springframework.stereotype.Service;
import progra2.SistemaMedico.dto.PagoDTO;
import progra2.SistemaMedico.model.enums.MetodoPago;

import java.math.BigDecimal;
import java.util.UUID;

// simulacion de pasarela de cobro de x banco esta implementara la forma de pago y si se agrega una pasarela de cobro real solo debe implementar la misma interface
@Service
public class PasarelaSimulada implements PasarelaDePago {
    private static final String RECHAZO_BANCARIO = "La transacción con tarjeta fue rechazada por el banco. Por favor, verifique los datos de su tarjeta o intente con una tarjeta diferente.";
    private static final String ERROR_PRCESAMIENTO = "El pago no pudo ser procesado. Por favor, intente nuevamente o utilice otra tarjeta.";
    private static final String ERROR_COMUNICACION = "Error de comunicación con la pasarela de pago. Intente nuevamente en unos minutos.";

    // FB PASOS 10-11 - CU-04 INICIO
    @Override
    public RespuestaPasarela cobrar(BigDecimal monto, PagoDTO datosTarjeta, String claveIdempotencia) {
        return decidirPorUltimoDigito(datosTarjeta.getNumeroTarjeta());
    }
    // FB PASOS 10-11 - CU-04 FINAL

    // FA01 - CU-06 INICIO
    // FA03 - CU-10 INICIO
    @Override
    public RespuestaPasarela cobrarEnTerminal(BigDecimal monto, MetodoPago tipo, String ultimosDigitos) {
        return decidirPorUltimoDigito(ultimosDigitos);
    }
    // FA03 - CU-10 FINAL
    // FA01 - CU-06 FINAL

    private RespuestaPasarela decidirPorUltimoDigito(String numero) {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        char ultimo = numero.charAt(numero.length() - 1);

        switch (ultimo) {
            // FA03 - CU-04 INICIO
            case '0':
                return new RespuestaPasarela(false, null, RECHAZO_BANCARIO);
            case '1':
                return new RespuestaPasarela(false, null, ERROR_PRCESAMIENTO);
            case '2':
                return new RespuestaPasarela(false, null, ERROR_COMUNICACION);
                // FA03 - CU-04 FINAL
            // FB PASO 11 - CU-04 INICIO
            default:
                String autorizacion = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                return new RespuestaPasarela(true, autorizacion, null);
                // FB PASO 11 - CU-04 FINAL
        }
    }
}
