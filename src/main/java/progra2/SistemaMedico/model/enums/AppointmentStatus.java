package progra2.SistemaMedico.model.enums;

    public enum AppointmentStatus {
        PENDIENTE_PAGO("Pendiente de pago"),
        PAGADA("Pagada"),
        CANCELADA("Cancelada"),
        COMPLETADA("Completada"),
        REPROGRAMADA("Reprogramada");

        private final String descripcion;

        AppointmentStatus(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

