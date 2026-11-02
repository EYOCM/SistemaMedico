package progra2.SistemaMedico.model.enums;

public enum EstadoConsulta {
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada");

    private final String descripcion;

    EstadoConsulta(String descripcion){
        this.descripcion = descripcion;
    }
    public String getDescripcion() {
        return descripcion;
    }


}
