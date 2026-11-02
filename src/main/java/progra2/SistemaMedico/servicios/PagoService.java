package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.PagoDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.pasarela.PasarelaDePago;
import progra2.SistemaMedico.pasarela.RespuestaPasarela;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.servicios.serviceImpl.IEmailService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PagoService {
    private final CitaRepositorio citaRepo;
    private final PasarelaDePago pasarela;
    private final AuditoriaService auditoria;
    private final CitaMapper mapper;
    private final IEmailService emailService;

    private static final String MENSAJE_FA02 =
            "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. "
                    + "Por favor, seleccione un nuevo horario. Será redirigido en unos segundos...";

    // FB PASOS 10-14 - CU-04 INICIO
    @Transactional
    public CitaDTO procesarPago(Long citaId, PagoDTO datos, String clave) {
        // buscar la cita
        Cita cita = citaRepo.findById(citaId).orElseThrow(() -> new ReglasDeNegocioException("La cita con id: " + citaId + " no existe"));
        // si ya esta pagada se devuelve sin cobrar
        if (cita.getEstado() == AppointmentStatus.PAGADA) return mapper.convertirADTO(cita);
        // FA02 - CU-04 INICIO
        //validar vigencia de los 5 min de reseva
        if (cita.getFechaReservaExpira() != null && cita.getFechaReservaExpira().isBefore(LocalDateTime.now()))
        throw new ReglasDeNegocioException(MENSAJE_FA02);
        // FA02 - CU-04 FINAL
        // pendiente de pgao
        if(cita.getEstado() != AppointmentStatus.PENDIENTE_PAGO) throw new ReglasDeNegocioException("Esta cita no se puede pagar");
        BigDecimal monto = cita.getEspecialidad().getPrecioConsulta();
        if(monto == null) throw new ReglasDeNegocioException("La especialidad no tiene un precio configurado");

        RespuestaPasarela respuesta = pasarela.cobrar(monto, datos, clave);

        // FA03 - CU-04 INICIO
        // respuesta rechazada no se modifica la cita para que el cliente pueda reintentar con el tiempo de reserva
        if(!respuesta.aprobada()) throw new ReglasDeNegocioException(respuesta.mensajeError());
        // FA03 - CU-04 FINAL
        // FB PASO 12 - CU-04 INICIO
        // respuesta aprobada se acutaliza la cita con la info
            cita.setEstado(AppointmentStatus.PAGADA);
            cita.setMontoPago(monto);
            cita.setNumeroTransaccion(respuesta.numeroAutorizacion()); // numero que dio la pasarela
            cita.setFechaPago(LocalDateTime.now());
            // la consulta de horarios libera las PENDIENTE DE PAGO vencidas, con null las citas pagadas bloquean el horario para que nadie mas agende con el mismo
            cita.setFechaReservaExpira(null);
            citaRepo.save(cita);
            auditoria.registrar("PAGO", "Cita", cita.getId(), "Pago aprobado. Autorizacion " + respuesta.numeroAutorizacion() + ", Monto Q " + monto);
            // FB PASO 12 - CU-04 FINAL
            // devolver DTO
            CitaDTO citaPagada = mapper.convertirADTO(cita);
            // FB PASO 14 - CU-04 INICIO
            emailService.enviarComprobantePago(citaPagada, cita.getPaciente().getEmail());
            // FB PASO 14 - CU-04 FINAL
            return citaPagada;


    }
    // FB PASOS 10-14 - CU-04 FINAL


}
