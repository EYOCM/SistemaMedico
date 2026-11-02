package progra2.SistemaMedico.servicios;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.SeguimientoCorreoDTO;
import progra2.SistemaMedico.servicios.serviceImpl.IEmailService;

@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.url-base}")
    private String urlBase;

    @Value("${app.telefono-contacto}")
    private String telefonoContacto;

    @Value("${app.nombre-hospital}")
    private String nombreHospital;


    // FB PASO 14 - CU-02 INICIO
    @Override
    @Async
    public void enviarCorreoBienvenida(String destinatario, String nombrePaciente) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Bienvenido al Sistema de Citas - Hospital General");

            Context context = new Context();
            context.setVariable("nombre", nombrePaciente);
            context.setVariable("urlLogin", urlBase + "/login");
            context.setVariable("telefonoContacto", telefonoContacto);
            String htmlContent = templateEngine.process("email-bienvenida", context);
            helper.setText(htmlContent, true);

            mailSender.send(mensaje);
            System.out.println("Correo enviado exitosamente a: " + destinatario);

        } catch (Exception e) {

            System.err.println("Error al enviar notificación por correo electrónico al paciente "
                    + nombrePaciente + ". Se reintentará automáticamente. Detalle: " + e.getMessage());
        }

    }
    // FB PASO 14 - CU-02 FINAL

    // FB PASO 14 - CU-04 INICIO
    @Override
    @Async
    public void enviarComprobantePago(CitaDTO citaDTO, String emailPaciente) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(emailPaciente);
            helper.setSubject("Comprobante de Pago - Cita Médica - " + nombreHospital);

            Context context = new Context();
            context.setVariable("cita", citaDTO);
            context.setVariable("nombreHospital", nombreHospital);
            context.setVariable("urlPortal", urlBase + "/paciente/dashboard");
            context.setVariable("telefonoContacto", telefonoContacto);

            String htmlContent = templateEngine.process("email-comprobante", context);
            helper.setText(htmlContent, true);

            mailSender.send(mensaje);
            System.out.println("Comprobante enviado a: " + emailPaciente);

        } catch (Exception e) {
            System.err.println("No se pudo enviar el comprobante de la cita "
                    + citaDTO.getNumeroCita() + ". Detalle: " + e.getMessage());
        }
    }
    // FB PASO 14 - CU-04 FINAL

    // FB PASO 9 - CU-12 INICIO
    @Override
    @Async
    public void enviarSeguimientoAgendado(SeguimientoCorreoDTO datos) {
        enviarSeguimiento(datos, false);
    }
    // FB PASO 9 - CU-12 FINAL

    // FB PASO 10 - CU-12 INICIO
    @Override
    public boolean enviarRecordatorioSeguimiento(SeguimientoCorreoDTO datos) {
        return enviarSeguimiento(datos, true);
    }
    // FB PASO 10 - CU-12 FINAL

    private boolean enviarSeguimiento(SeguimientoCorreoDTO datos, boolean recordatorio) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(datos.email());
            helper.setSubject(recordatorio
                    ? "Recordatorio: Su Cita de Seguimiento Mañana"
                    : "Cita de Seguimiento Agendada - " + nombreHospital);

            Context context = new Context();
            context.setVariable("datos", datos);
            context.setVariable("recordatorio", recordatorio);
            context.setVariable("nombreHospital", nombreHospital);
            context.setVariable("urlPago", urlBase + "/pago/" + datos.citaId());
            context.setVariable("telefonoContacto", telefonoContacto);
            helper.setText(templateEngine.process("email-seguimiento", context), true);

            mailSender.send(mensaje);
            System.out.println((recordatorio ? "Recordatorio" : "Aviso") + " de seguimiento enviado a: " + datos.email());
            return true;

        } catch (Exception e) {
            System.err.println("Error al enviar notificación por correo electrónico al paciente "
                    + datos.nombrePaciente() + ". Se reintentará automáticamente. Detalle: " + e.getMessage());
            return false;
        }
    }
}
