package progra2.SistemaMedico.servicios.serviceImpl;

import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.SeguimientoCorreoDTO;

public interface IEmailService {
    void enviarCorreoBienvenida(String destinatario, String nombrePaciente);

    void enviarComprobantePago(CitaDTO citaDTO, String emailPaciente);

    void enviarSeguimientoAgendado(SeguimientoCorreoDTO datos);

    boolean enviarRecordatorioSeguimiento(SeguimientoCorreoDTO datos);
}
