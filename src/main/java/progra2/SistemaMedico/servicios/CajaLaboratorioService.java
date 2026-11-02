package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.OrdenLaboratorio;
import progra2.SistemaMedico.model.Pago;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.ConceptoPago;
import progra2.SistemaMedico.model.enums.EstadoOrdenLaboratorio;
import progra2.SistemaMedico.repositorio.OrdenLaboratorioRepositorio;
import progra2.SistemaMedico.repositorio.PagoRepositorio;

import java.time.LocalDateTime;
import progra2.SistemaMedico.util.ValidacionDpi;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaLaboratorioService {

    private final OrdenLaboratorioRepositorio ordenRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final ProcesadorDeCobro procesadorDeCobro;
    private final AuditoriaService auditoriaService;

    // FB PASOS 2-3 - CU-10 INICIO
    @Transactional(readOnly = true)
    public List<OrdenCobroDTO> buscarPorNumero(String numero) {
        return ordenRepositorio.findByNumeroOrden(numero.trim().toUpperCase())
                .filter(this::esCobrable)
                .map(this::convertir)
                .stream()
                .toList();
    }
    // FB PASOS 2-3 - CU-10 FINAL

    // FB PASOS 2-3 - CU-10 INICIO
    @Transactional(readOnly = true)
    public List<OrdenCobroDTO> buscarPorDpi(String dpi) {
        return ordenRepositorio
                .findByCitaPacienteDpiAndEstadoAndExternaFalseOrderByFechaCreacionAsc(
                        ValidacionDpi.validar(dpi), EstadoOrdenLaboratorio.PENDIENTE)
                .stream()
                .map(this::convertir)
                .toList();
    }
    // FB PASOS 2-3 - CU-10 FINAL

    private boolean esCobrable(OrdenLaboratorio orden) {
        return orden.getEstado() == EstadoOrdenLaboratorio.PENDIENTE && !orden.getExterna();
    }

    private OrdenCobroDTO convertir(OrdenLaboratorio orden) {
        Usuario paciente = orden.getCita().getPaciente();
        Usuario medico = orden.getCita().getMedico();

        List<OrdenExamenLineaDTO> examenes = orden.getExamenes().stream()
                .map(oe -> OrdenExamenLineaDTO.builder()
                        .nombre(oe.getExamen().getNombre())
                        .precio(oe.getPrecio())
                        .build())
                .toList();

        return OrdenCobroDTO.builder()
                .id(orden.getId())
                .numeroOrden(orden.getNumeroOrden())
                .nombrePaciente(paciente.getNombre() + " " + paciente.getApellidos())
                .dpi(paciente.getDpi())
                .nombreMedico(medico.getNombre() + " " + medico.getApellidos())
                .fechaCreacion(orden.getFechaCreacion())
                .total(orden.getTotal())
                .examenes(examenes)
                .build();
    }
    // FB PASO 5 - CU-10 INICIO
    @Transactional(readOnly = true)
    public OrdenCobroDTO obtenerOrdenParaCobro(Long ordenId) {
        return convertir(buscarCobrable(ordenId));
    }
    // FB PASO 5 - CU-10 FINAL

    // FB PASOS 6-9 - CU-10 INICIO
    @Transactional
    public Long registrarCobro(Long ordenId, CobroCajaDTO datos, String cajero) {
        OrdenLaboratorio orden = buscarCobrable(ordenId);

        ResultadoCobro cobro = procesadorDeCobro.procesar(orden.getTotal(), datos);

        // FB PASO 6 - CU-09 INICIO
        // FB PASO 9 - CU-10 INICIO
        orden.setEstado(EstadoOrdenLaboratorio.EN_PROCESO);
        ordenRepositorio.saveAndFlush(orden);
        // FB PASO 6 - CU-09 FINAL

        Pago pago = pagoRepositorio.save(Pago.builder()
                .cita(orden.getCita())
                .orden(orden)
                .concepto(ConceptoPago.LABORATORIO)
                .metodo(cobro.metodo())
                .monto(orden.getTotal())
                .montoRecibido(cobro.montoRecibido())
                .cambio(cobro.cambio())
                .ultimosDigitos(cobro.ultimosDigitos())
                .numeroTransaccion(cobro.numeroTransaccion())
                .fechaPago(LocalDateTime.now())
                .registradoPor(cajero)
                .build());

        auditoriaService.registrar("PAGO_LABORATORIO", "OrdenLaboratorio", orden.getId(),
                "Cobro de orden " + orden.getNumeroOrden() + ". " + cobro.metodo().getDescripcion() + ", "
                        + ProcesadorDeCobro.formatear(orden.getTotal()) + ", transacción " + cobro.numeroTransaccion());
        // FB PASO 9 - CU-10 FINAL

        return pago.getId();
    }
    // FB PASOS 6-9 - CU-10 FINAL

    // FB PASO 10 - CU-10 INICIO
    @Transactional(readOnly = true)
    public ComprobanteLaboratorioDTO obtenerComprobante(Long pagoId) {
        Pago pago = pagoRepositorio.findById(pagoId)
                .filter(p -> p.getOrden() != null)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el pago de laboratorio."));

        return ComprobanteLaboratorioDTO.builder()
                .orden(convertir(pago.getOrden()))
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
    // FB PASO 10 - CU-10 FINAL

    // FB PASO 5 - CU-10 INICIO
    private OrdenLaboratorio buscarCobrable(Long ordenId) {
        OrdenLaboratorio orden = ordenRepositorio.findById(ordenId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la orden de laboratorio."));
        if (orden.getExterna()) {
            throw new ReglasDeNegocioException("Las órdenes externas no se cobran en caja.");
        }
        if (orden.getEstado() != EstadoOrdenLaboratorio.PENDIENTE) {
            throw new ReglasDeNegocioException("Esta orden ya fue pagada.");
        }
        return orden;
    }
    // FB PASO 5 - CU-10 FINAL
}