package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import progra2.SistemaMedico.dto.CobroCajaDTO;
import progra2.SistemaMedico.dto.ResultadoCobro;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.enums.MetodoPago;
import progra2.SistemaMedico.pasarela.PasarelaDePago;
import progra2.SistemaMedico.pasarela.RespuestaPasarela;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcesadorDeCobro {

    private static final String MENSAJE_FA04 =
            "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.";

    private final PasarelaDePago pasarelaDePago;

    // FB PASOS 4-7 - CU-06 INICIO
    // FB PASOS 6-7 - CU-10 INICIO
    public ResultadoCobro procesar(BigDecimal total, CobroCajaDTO datos) {
        MetodoPago metodo = datos.getMetodo();
        if (metodo == null || !MetodoPago.enCaja().contains(metodo)) {
            throw new ReglasDeNegocioException("Debe seleccionar un método de pago.");
        }

        // FB PASOS 5-7 - CU-06 INICIO
        // FB PASO 7 - CU-10 INICIO
        if (metodo == MetodoPago.EFECTIVO) {
            BigDecimal recibido = datos.getMontoRecibido();
            if (recibido == null) {
                throw new ReglasDeNegocioException("Debe ingresar el monto recibido.");
            }
            if (recibido.compareTo(total) < 0) {
                throw new ReglasDeNegocioException("El monto recibido (" + formatear(recibido)
                        + ") es menor al monto a cobrar (" + formatear(total) + ")");
            }
            String numero = "CAJA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new ResultadoCobro(metodo, recibido, recibido.subtract(total), null, numero);
        }
        // FB PASO 7 - CU-10 FINAL
        // FB PASOS 5-7 - CU-06 FINAL

        // FA01 - CU-06 INICIO
        // FA03 - CU-10 INICIO
        String digitos = datos.getUltimosDigitos();
        if (digitos == null || !digitos.matches("\\d{4}")) {
            throw new ReglasDeNegocioException("Ingrese los últimos 4 dígitos de la tarjeta.");
        }
        RespuestaPasarela respuesta = pasarelaDePago.cobrarEnTerminal(total, metodo, digitos);
        // FA04 - CU-06 INICIO
        // FA04 - CU-10 INICIO
        if (!respuesta.aprobada()) {
            throw new ReglasDeNegocioException(MENSAJE_FA04);
        }
        // FA04 - CU-10 FINAL
        // FA04 - CU-06 FINAL
        return new ResultadoCobro(metodo, null, null, digitos, respuesta.numeroAutorizacion());
        // FA03 - CU-10 FINAL
        // FA01 - CU-06 FINAL
    }
    // FB PASOS 6-7 - CU-10 FINAL
    // FB PASOS 4-7 - CU-06 FINAL

    public static String formatear(BigDecimal monto) {
        return String.format("Q%.2f", monto);
    }
}