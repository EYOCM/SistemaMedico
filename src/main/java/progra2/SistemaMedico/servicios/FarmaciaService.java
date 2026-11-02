package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.*;
import progra2.SistemaMedico.model.enums.EstadoDespacho;
import progra2.SistemaMedico.model.enums.EstadoReceta;
import progra2.SistemaMedico.model.enums.TipoMovimiento;
import progra2.SistemaMedico.repositorio.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmaciaService {

    public static final int DIAS_VIGENCIA = 7;

    private final RecetaRepositorio recetaRepositorio;
    private final MedicamentoRepositorio medicamentoRepositorio;
    private final InventarioMedicamentoRepositorio inventarioRepositorio;
    private final MovimientoInventarioRepositorio movimientoRepositorio;
    private final DespachoRepositorio despachoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final AuditoriaService auditoriaService;

    // FB PASO 2 - CU-11 INICIO
    @Transactional(readOnly = true)
    public List<RecetaBusquedaDTO> buscar(String tipo, String valor) {
        String texto = valor.trim().toUpperCase();
        Optional<Receta> receta = "consulta".equals(tipo)
                ? recetaRepositorio.findByCitaNumeroCitaAndEstado(texto, EstadoReceta.PENDIENTE)
                : recetaRepositorio.findByNumeroRecetaAndEstado(texto, EstadoReceta.PENDIENTE);
        return receta.map(this::aBusqueda).stream().toList();
    }
    // FB PASO 2 - CU-11 FINAL

    // FB PASOS 4-6 - CU-11 INICIO
    @Transactional(readOnly = true)
    public PreparacionDespachoDTO preparar(Long recetaId, String username) {
        Receta receta = buscarDespachable(recetaId, true);
        Sucursal sucursal = sucursalDe(username);

        List<DespachoLineaDTO> lineas = receta.getDetalles().stream()
                .sorted(Comparator.comparing(RecetaDetalle::getId))
                .map(d -> {
                    Medicamento m = d.getMedicamento();
                    // FA01 - CU-11 INICIO
                    Integer stock = inventarioRepositorio
                            .findByMedicamentoIdAndSucursalIdAndActivoTrue(m.getId(), sucursal.getId())
                            .map(InventarioMedicamento::getStockActual)
                            .orElse(null);
                    // FA01 - CU-11 FINAL
                    return DespachoLineaDTO.builder()
                            .recetaDetalleId(d.getId())
                            .nombreMedicamento(m.getNombre())
                            .unidad(m.getUnidad())
                            .dosis(d.getDosis())
                            .frecuencia(d.getFrecuencia())
                            .duracion(d.getDuracion())
                            .indicaciones(d.getIndicaciones())
                            .precioUnitario(m.getPrecio())
                            .stockDisponible(stock)
                            .stockMinimo(m.getStockMinimo())
                            .cantidad(stock != null && stock > 0 ? 1 : 0)
                            .build();
                })
                .toList();

        return PreparacionDespachoDTO.builder()
                .receta(aBusqueda(receta))
                .nombreSucursal(sucursal.getNombre())
                .formulario(DespachoFormDTO.builder().lineas(new ArrayList<>(lineas)).build())
                .alternativas(medicamentoRepositorio.findByActivoTrueOrderByNombreAsc())
                .build();
    }
    // FB PASOS 4-6 - CU-11 FINAL

    // FB PASOS 9-10 - CU-11 INICIO
    @Transactional
    public Long confirmar(Long recetaId, DespachoFormDTO form, String username) {
        Receta receta = buscarDespachable(recetaId, true);
        Sucursal sucursal = sucursalDe(username);

        Map<Long, RecetaDetalle> recetados = receta.getDetalles().stream()
                .collect(Collectors.toMap(RecetaDetalle::getId, Function.identity()));

        List<DespachoLineaDTO> aDespachar = form.getLineas().stream()
                .filter(Objects::nonNull)
                .filter(l -> l.getCantidad() != null && l.getCantidad() > 0)
                .toList();
        if (aDespachar.isEmpty()) {
            throw new ReglasDeNegocioException("Indique la cantidad de al menos un medicamento a despachar.");
        }

        // FB PASO 10 - CU-11 INICIO
        LocalDateTime ahora = LocalDateTime.now();
        Despacho despacho = Despacho.builder()
                .numeroDespacho(generarNumero())
                .receta(receta)
                .sucursal(sucursal)
                .estado(EstadoDespacho.COMPLETADO)
                .registradoPor(username)
                .fecha(ahora)
                .total(BigDecimal.ZERO)
                .build();
        List<MovimientoInventario> movimientos = new ArrayList<>();

        for (DespachoLineaDTO linea : aDespachar) {
            RecetaDetalle recetado = recetados.get(linea.getRecetaDetalleId());
            if (recetado == null) {
                throw new ReglasDeNegocioException("El medicamento no pertenece a esta receta.");
            }
            // FA02 - CU-11 INICIO
            Medicamento medicamento = linea.isSustituir()
                    ? sustituto(recetado.getMedicamento(), linea)
                    : recetado.getMedicamento();
            // FA02 - CU-11 FINAL

            // FA01 - CU-11 INICIO
            InventarioMedicamento inventario = inventarioRepositorio
                    .findByMedicamentoIdAndSucursalIdAndActivoTrue(medicamento.getId(), sucursal.getId())
                    .orElseThrow(() -> new ReglasDeNegocioException(
                            "Sin inventario registrado para " + medicamento.getNombre() + "."));
            if (inventario.getStockActual() < linea.getCantidad()) {
                throw new ReglasDeNegocioException("Stock insuficiente para " + medicamento.getNombre()
                        + ". Disponible: " + inventario.getStockActual() + ", solicitado: " + linea.getCantidad() + ".");
            }
            // FA01 - CU-11 FINAL

            int anterior = inventario.getStockActual();
            inventario.setStockActual(anterior - linea.getCantidad());

            BigDecimal subtotal = medicamento.getPrecio().multiply(BigDecimal.valueOf(linea.getCantidad()));
            despacho.agregarDetalle(DespachoDetalle.builder()
                    .recetaDetalle(recetado)
                    .medicamento(medicamento)
                    .cantidad(linea.getCantidad())
                    .precioUnitario(medicamento.getPrecio())
                    .subtotal(subtotal)
                    .sustituido(linea.isSustituir())
                    .razonSustitucion(linea.isSustituir() ? linea.getRazonSustitucion().trim() : null)
                    .build());
            despacho.setTotal(despacho.getTotal().add(subtotal));

            movimientos.add(MovimientoInventario.builder()
                    .tipo(TipoMovimiento.DESPACHO)
                    .medicamento(medicamento)
                    .sucursal(sucursal)
                    .cantidad(linea.getCantidad())
                    .stockAnterior(anterior)
                    .stockNuevo(inventario.getStockActual())
                    .referencia(despacho.getNumeroDespacho())
                    .motivo("Receta " + receta.getNumeroReceta())
                    .usuario(username)
                    .fecha(ahora)
                    .activo(true)
                    .build());
        }

        receta.setEstado(EstadoReceta.DESPACHADA);
        recetaRepositorio.saveAndFlush(receta);
        inventarioRepositorio.flush();
        despachoRepositorio.saveAndFlush(despacho);
        movimientoRepositorio.saveAll(movimientos);
        // FB PASO 10 - CU-11 FINAL

        auditoriaService.registrar("DESPACHO_FARMACIA", "Receta", receta.getId(),
                "Despacho " + despacho.getNumeroDespacho() + ", " + despacho.getDetalles().size()
                        + " medicamento(s), Q" + despacho.getTotal());
        return despacho.getId();
    }
    // FB PASOS 9-10 - CU-11 FINAL

    // FA03 - CU-11 INICIO
    @Transactional
    public String registrarNoAdquirido(Long recetaId, String username) {
        Receta receta = buscarDespachable(recetaId, false);
        Sucursal sucursal = sucursalDe(username);

        despachoRepositorio.save(Despacho.builder()
                .numeroDespacho(generarNumero())
                .receta(receta)
                .sucursal(sucursal)
                .estado(EstadoDespacho.NO_ADQUIRIDO)
                .total(BigDecimal.ZERO)
                .registradoPor(username)
                .fecha(LocalDateTime.now())
                .build());
        receta.setEstado(EstadoReceta.NO_ADQUIRIDA);
        recetaRepositorio.saveAndFlush(receta);

        String mensaje = "Se ha registrado que el paciente " + nombreCompleto(receta.getCita().getPaciente())
                + " no adquirió los medicamentos recetados en farmacia interna. Receta: " + receta.getNumeroReceta() + ".";
        auditoriaService.registrar("RECETA_NO_ADQUIRIDA", "Receta", receta.getId(), mensaje);
        return mensaje;
    }
    // FA03 - CU-11 FINAL

    // FB PASO 11 - CU-11 INICIO
    @Transactional(readOnly = true)
    public DespachoResumenDTO resumen(Long despachoId) {
        Despacho despacho = despachoRepositorio.findById(despachoId)
                .filter(d -> d.getEstado() == EstadoDespacho.COMPLETADO)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el despacho."));

        List<DespachoResumenLineaDTO> lineas = new ArrayList<>();
        List<String> sustituciones = new ArrayList<>();
        Set<String> alertas = new LinkedHashSet<>();

        for (DespachoDetalle d : despacho.getDetalles()) {
            Medicamento entregado = d.getMedicamento();
            Medicamento recetado = d.getRecetaDetalle().getMedicamento();
            lineas.add(DespachoResumenLineaDTO.builder()
                    .nombre(entregado.getNombre())
                    .cantidad(d.getCantidad())
                    .precioUnitario(d.getPrecioUnitario())
                    .subtotal(d.getSubtotal())
                    .sustituyeA(d.getSustituido() ? recetado.getNombre() : null)
                    .razonSustitucion(d.getRazonSustitucion())
                    .build());
            // FA02 - CU-11 INICIO
            if (d.getSustituido()) {
                sustituciones.add("Medicamento " + recetado.getNombre() + " sustituido por " + entregado.getNombre()
                        + ". El médico tratante será notificado de la sustitución.");
            }
            // FA02 - CU-11 FINAL
            // FA04 - CU-11 INICIO
            inventarioRepositorio
                    .findByMedicamentoIdAndSucursalIdAndActivoTrue(entregado.getId(), despacho.getSucursal().getId())
                    .filter(inv -> inv.getStockActual() <= entregado.getStockMinimo())
                    .ifPresent(inv -> alertas.add("ALERTA: El medicamento " + entregado.getNombre()
                            + " ha alcanzado el nivel de stock mínimo (" + inv.getStockActual()
                            + " unidades restantes). Se recomienda generar orden de reabastecimiento."));
            // FA04 - CU-11 FINAL
        }

        return DespachoResumenDTO.builder()
                .id(despacho.getId())
                .numeroDespacho(despacho.getNumeroDespacho())
                .numeroReceta(despacho.getReceta().getNumeroReceta())
                .nombrePaciente(nombreCompleto(despacho.getReceta().getCita().getPaciente()))
                .nombreSucursal(despacho.getSucursal().getNombre())
                .fecha(despacho.getFecha())
                .total(despacho.getTotal())
                .registradoPor(despacho.getRegistradoPor())
                .lineas(lineas)
                .sustituciones(sustituciones)
                .alertasStock(new ArrayList<>(alertas))
                .build();
    }
    // FB PASO 11 - CU-11 FINAL

    // FB PASO 4 - CU-11 INICIO
    private Receta buscarDespachable(Long recetaId, boolean exigirVigencia) {
        Receta receta = recetaRepositorio.findById(recetaId)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró la receta."));
        if (receta.getEstado() != EstadoReceta.PENDIENTE) {
            throw new ReglasDeNegocioException("La receta " + receta.getNumeroReceta() + " ya fue procesada ("
                    + receta.getEstado().getDescripcion().toLowerCase() + ").");
        }
        long dias = diasDesde(receta);
        if (exigirVigencia && dias > DIAS_VIGENCIA) {
            throw new ReglasDeNegocioException("Receta Vencida. La receta #" + receta.getNumeroReceta()
                    + " fue emitida hace " + dias + " días y ya no es válida para despacho.");
        }
        return receta;
    }
    // FB PASO 4 - CU-11 FINAL

    // FA02 - CU-11 INICIO
    private Medicamento sustituto(Medicamento original, DespachoLineaDTO linea) {
        if (linea.getMedicamentoSustitutoId() == null || linea.getRazonSustitucion() == null
                || linea.getRazonSustitucion().isBlank()) {
            throw new ReglasDeNegocioException("Para sustituir " + original.getNombre()
                    + " indique el medicamento alternativo y la razón de sustitución.");
        }
        Medicamento alternativo = medicamentoRepositorio.findById(linea.getMedicamentoSustitutoId())
                .filter(Medicamento::getActivo)
                .orElseThrow(() -> new ReglasDeNegocioException("El medicamento alternativo no existe en el catálogo."));
        if (alternativo.getId().equals(original.getId())) {
            throw new ReglasDeNegocioException("El medicamento alternativo debe ser distinto al recetado.");
        }
        return alternativo;
    }
    // FA02 - CU-11 FINAL

    private Sucursal sucursalDe(String username) {
        Usuario usuario = usuarioRepositorio.findByUsername(username)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el usuario."));
        if (usuario.getSucursal() == null) {
            throw new ReglasDeNegocioException("Su usuario no tiene una sucursal asignada.");
        }
        return usuario.getSucursal();
    }

    // FB PASOS 2-3 - CU-11 INICIO
    private RecetaBusquedaDTO aBusqueda(Receta receta) {
        long dias = diasDesde(receta);
        Usuario paciente = receta.getCita().getPaciente();
        return RecetaBusquedaDTO.builder()
                .id(receta.getId())
                .numeroReceta(receta.getNumeroReceta())
                .numeroCita(receta.getCita().getNumeroCita())
                .nombrePaciente(nombreCompleto(paciente))
                .dpi(paciente.getDpi())
                .nombreMedico(nombreCompleto(receta.getCita().getMedico()))
                .fechaEmision(receta.getFechaEmision())
                .diasTranscurridos(dias)
                .vigente(dias <= DIAS_VIGENCIA)
                .cantidadMedicamentos(receta.getDetalles().size())
                .build();
    }
    // FB PASOS 2-3 - CU-11 FINAL

    private long diasDesde(Receta receta) {
        return ChronoUnit.DAYS.between(receta.getFechaEmision().toLocalDate(), LocalDate.now());
    }

    private String nombreCompleto(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellidos();
    }

    private String generarNumero() {
        String fecha = LocalDate.now().toString().replace("-", "");
        int aleatorio = (int) (Math.random() * 9000) + 1000;
        return "DESP-" + fecha + "-" + aleatorio;
    }
}
