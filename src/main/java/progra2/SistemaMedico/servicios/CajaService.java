package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.CitaMapper;
import progra2.SistemaMedico.model.Cita;
import progra2.SistemaMedico.model.Pago;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.ConceptoPago;
import progra2.SistemaMedico.repositorio.CitaRepositorio;
import progra2.SistemaMedico.repositorio.PagoRepositorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// buscar pendientes, validar si una cita se puede cobrar, cobrarla y armar el comprobante
@Service
@RequiredArgsConstructor
public class CajaService {

    private static final List<AppointmentStatus> SOLO_PENDIENTES = List.of(AppointmentStatus.PENDIENTE_PAGO);

    private final BusquedaCitaService busquedaCitaService;
    private final CitaRepositorio citaRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final ProcesadorDeCobro procesadorDeCobro;
    private final AuditoriaService auditoriaService;
    private final CitaMapper mapper;

    // FB PASO 1 - CU-06 INICIO
    public ResultadoBusquedaCitaDTO buscarPorNumero(String numero) {
        return busquedaCitaService.buscarPorNumero(numero, SOLO_PENDIENTES);
    }
    // FB PASO 1 - CU-06 FINAL

    // FB PASO 1 - CU-06 INICIO
    public ResultadoBusquedaCitaDTO buscarPorDpi(String dpi) {
        return busquedaCitaService.buscarPorDpi(dpi, SOLO_PENDIENTES);
    }
    // FB PASO 1 - CU-06 FINAL

    // FB PASO 2 - CU-06 INICIO
    @Transactional(readOnly = true)
    public CitaDTO obtenerCitaParaCobro(Long citaId) {
        return mapper.convertirADTO(buscarCobrable(citaId));
    }
    // FB PASO 2 - CU-06 FINAL

    // FB PASO 9 - CU-06 INICIO
    @Transactional(readOnly = true)
    public ComprobanteCajaDTO obtenerComprobante(Long pagoId) {
        Pago pago = pagoRepositorio.findById(pagoId)
                .filter(p -> p.getOrden() == null)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el pago."));

        return ComprobanteCajaDTO.builder()
                .cita(mapper.convertirADTO(pago.getCita()))
                .metodo(pago.getMetodo().getDescripcion())
                .monto(pago.getMonto())
                .montoRecibido(pago.getMontoRecibido())
                .cambio(pago.getCambio())
                .ultimosDigitos(pago.getUltimosDigitos())
                .numeroTransaccion(pago.getNumeroTransaccion())
                .fechaPago(pago.getFechaPago())
                .registradoPor(pago.getRegistradoPor())
                .build();
    }
    // FB PASO 9 - CU-06 FINAL

    // FB PASO 2 - CU-06 INICIO
    private Cita buscarCobrable(Long citaId) {
        Cita cita = citaRepositorio.findById(citaId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la cita."));

        if (cita.getEstado() == AppointmentStatus.PAGADA) {
            throw new ReglasDeNegocioException("Esta cita ya fue pagada.");
        }
        if (cita.getEstado() != AppointmentStatus.PENDIENTE_PAGO) {
            throw new ReglasDeNegocioException(
                    "Solo se pueden cobrar citas con estado 'Pendiente de pago'.");
        }
        if (!busquedaCitaService.estaVigente(cita)) {
            throw new ReglasDeNegocioException(
                    "La reserva de esta cita venció y el horario fue liberado. El paciente debe agendar una nueva cita.");
        }
        return cita;
    }
    // FB PASO 2 - CU-06 FINAL


    // FB PASOS 6-8 - CU-06 INICIO
    @Transactional
    public Long registrarCobro(Long citaId, CobroCajaDTO datos, String cajero) {
        Cita cita = buscarCobrable(citaId);

        BigDecimal total = cita.getEspecialidad().getPrecioConsulta();
        if (total == null) {
            throw new ReglasDeNegocioException("La especialidad no tiene un precio configurado.");
        }

        ResultadoCobro cobro = procesadorDeCobro.procesar(total, datos);

        // FB PASO 8 - CU-06 INICIO
        LocalDateTime ahora = LocalDateTime.now();

        cita.setEstado(AppointmentStatus.PAGADA);
        cita.setMontoPago(total);
        cita.setNumeroTransaccion(cobro.numeroTransaccion());
        cita.setFechaPago(ahora);
        cita.setFechaReservaExpira(null);
        citaRepositorio.saveAndFlush(cita);

        Pago guardado = pagoRepositorio.save(Pago.builder()
                .cita(cita)
                .concepto(ConceptoPago.CONSULTA)
                .metodo(cobro.metodo())
                .monto(total)
                .montoRecibido(cobro.montoRecibido())
                .cambio(cobro.cambio())
                .ultimosDigitos(cobro.ultimosDigitos())
                .numeroTransaccion(cobro.numeroTransaccion())
                .fechaPago(ahora)
                .registradoPor(cajero)
                .build());

        auditoriaService.registrar("PAGO_CAJA", "Cita", cita.getId(),
                "Cobro en caja. " + cobro.metodo().getDescripcion() + ", " + ProcesadorDeCobro.formatear(total)
                        + ", transacción " + cobro.numeroTransaccion());
        // FB PASO 8 - CU-06 FINAL

        return guardado.getId();
    }
    // FB PASOS 6-8 - CU-06 FINAL

}
