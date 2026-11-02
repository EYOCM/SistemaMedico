package progra2.SistemaMedico.excepciones;

public class HorarioNoDisponibleException extends ReglasDeNegocioException {

    public HorarioNoDisponibleException() {
        super("El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
    }
}
