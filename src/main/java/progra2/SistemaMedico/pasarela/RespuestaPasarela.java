package progra2.SistemaMedico.pasarela;

// clase para guardar datos que no cambian
public record RespuestaPasarela(boolean aprobada, // transaccion aceptada/rechazzada
                                String numeroAutorizacion, // comprobante
                                String mensajeError)// mensaje de error solo si fue rechazada)
                                {

}
