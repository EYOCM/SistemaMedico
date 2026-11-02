package progra2.SistemaMedico.excepciones;

// la cuenta esta bloqueada temporalmente por intentos fallidos.

public class CuentaBloqueadaException extends ReglasDeNegocioException {

    private final long minutosRestantes;

    public CuentaBloqueadaException(String message, long minutosRestantes) {
        super(message);
        this.minutosRestantes = minutosRestantes;
    }

    public long getMinutosRestantes() {
        return minutosRestantes;
    }
}
