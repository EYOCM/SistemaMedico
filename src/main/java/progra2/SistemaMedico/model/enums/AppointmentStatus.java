package progra2.SistemaMedico.model.enums;

/*
 * CU-14: los "Estados de Cita" se muestran en /admin/catalogos solo para consulta y NO se editan.
 * Cada estado es un paso del flujo que el código revisa directamente (CU-03 a CU-12):
 * por ejemplo, MedicoService solo inicia una consulta si la cita está EN_ESPERA.
 * Si el administrador pudiera borrar o renombrar un estado, esas validaciones dejarían de funcionar.
 */
    public enum AppointmentStatus {
        PENDIENTE_PAGO("Pendiente de pago"),
        PAGADA("Pagada"),
        CANCELADA("Cancelada"),
        REPROGRAMADA("Reprogramada"),
        PACIENTE_PRESENTE("Paciente presente"),
        SIGNOS_VITALES("Signos vitales"),
        EN_ESPERA("En espera de consulta"),
        CONSULTA_MEDICA("En consulta medica"),
        EVALUADO("Evaluado - pendiente de cierre"),
        NO_ASISTIO("No asistio"),
        ATENCION_FINALIZADA("Atencion finalizada")
        ;
        private final String descripcion;

        AppointmentStatus(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

