package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.InventarioMedicamento;
import progra2.SistemaMedico.model.Medicamento;
import progra2.SistemaMedico.model.MovimientoInventario;
import progra2.SistemaMedico.model.Sucursal;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.TipoMovimiento;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.*;
import progra2.SistemaMedico.util.Paginacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private static final String MENSAJE_MOTIVO = "El motivo debe contener entre 10 y 500 caracteres.";

    private final InventarioMedicamentoRepositorio inventarioRepositorio;
    private final MovimientoInventarioRepositorio movimientoRepositorio;
    private final MedicamentoRepositorio medicamentoRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final AuditoriaService auditoriaService;

    // ============================================================
    //  QUIÉN VE QUÉ: el farmacéutico solo su sede; el administrador todas
    // ============================================================

    @Transactional(readOnly = true)
    public AlcanceInventarioDTO alcance(String username) {
        Usuario usuario = usuarioRepositorio.findByUsername(username)
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el usuario."));
        if (usuario.getRol() == UserRol.ADMINISTRADOR) {
            return new AlcanceInventarioDTO(true, null, null);
        }
        Sucursal sede = usuario.getSucursal();
        if (sede == null) {
            throw new ReglasDeNegocioException("Su usuario no tiene una sucursal asignada.");
        }
        return new AlcanceInventarioDTO(false, sede.getId(), sede.getNombre());
    }

    // ============================================================
    //  INVENTARIO (stock por medicamento y sede)
    // ============================================================

    // FB PASO 1 - CU-15 INICIO
    @Transactional(readOnly = true)
    public Page<InventarioFilaDTO> listarInventario(String texto, String username, int pagina, int tam) {
        AlcanceInventarioDTO alcance = alcance(username);
        String buscar = texto == null ? "" : texto.trim();
        Sort orden = Sort.by("medicamento.nombre").and(Sort.by("sucursal.nombre"));
        return Paginacion.buscar(pagina, tam, orden, pageable -> alcance.administrador()
                ? inventarioRepositorio.findByMedicamentoNombreContainingIgnoreCaseAndActivoTrue(buscar, pageable)
                : inventarioRepositorio.findByMedicamentoNombreContainingIgnoreCaseAndSucursalIdAndActivoTrue(
                        buscar, alcance.sucursalId(), pageable))
                .map(this::aFila);
    }
    // FB PASO 1 - CU-15 FINAL

    // FB PASO 1 - CU-15 INICIO
    @Transactional(readOnly = true)
    public InventarioDetalleDTO detalleInventario(Long id, String username) {
        AlcanceInventarioDTO alcance = alcance(username);
        InventarioMedicamento inventario = inventarioRepositorio.findOneById(id)
                .filter(i -> puedeVer(alcance, i.getSucursal().getId()))
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el inventario."));
        Long medicamentoId = inventario.getMedicamento().getId();
        Long sucursalId = inventario.getSucursal().getId();

        LocalDate primeroDelMes = LocalDate.now().withDayOfMonth(1);
        Map<TipoMovimiento, Long> totales = new LinkedHashMap<>();
        for (Object[] fila : movimientoRepositorio.totalesPorTipo(medicamentoId, sucursalId,
                primeroDelMes.atStartOfDay(), primeroDelMes.plusMonths(1).atStartOfDay())) {
            totales.put((TipoMovimiento) fila[0], ((Number) fila[1]).longValue());
        }
        List<ResumenTipoDTO> resumen = totales.entrySet().stream()
                .map(e -> new ResumenTipoDTO(e.getKey().getDescripcion(), e.getKey().isEntrada(), e.getValue()))
                .toList();
        long neto = resumen.stream().mapToLong(r -> r.entrada() ? r.cantidad() : -r.cantidad()).sum();
        String mes = primeroDelMes.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                + " " + primeroDelMes.getYear();

        List<MovimientoFilaDTO> ultimos = movimientoRepositorio
                .findTop10ByMedicamentoIdAndSucursalIdOrderByFechaDescIdDesc(medicamentoId, sucursalId)
                .stream().map(this::aFila).toList();
        return new InventarioDetalleDTO(aFila(inventario), mes, resumen, neto, ultimos);
    }
    // FB PASO 1 - CU-15 FINAL

    // FB PASO 4 / FA05 - CU-15 INICIO
    @Transactional(readOnly = true)
    public StockInfoDTO stock(Long medicamentoId, Long sucursalId, String username) {
        AlcanceInventarioDTO alcance = alcance(username);
        Long sede = alcance.administrador() ? sucursalId : alcance.sucursalId();
        Medicamento medicamento = medicamentoRepositorio.findById(medicamentoId)
                .orElseThrow(() -> new ReglasDeNegocioException("El medicamento seleccionado no existe."));
        int minimo = minimo(medicamento);
        return inventarioRepositorio.findByMedicamentoIdAndSucursalIdAndActivoTrue(medicamentoId, sede)
                .map(i -> new StockInfoDTO(medicamento.getNombre(), i.getStockActual(), minimo, true))
                .orElseGet(() -> new StockInfoDTO(medicamento.getNombre(), 0, minimo, false));
    }
    // FB PASO 4 / FA05 - CU-15 FINAL

    // ============================================================
    //  BITÁCORA
    // ============================================================

    // FB PASO 2 - CU-15 INICIO
    @Transactional(readOnly = true)
    public Page<MovimientoFilaDTO> buscarMovimientos(FiltroMovimientoDTO filtro, String username, int pagina, int tam) {
        if (filtro.getDesde() != null && filtro.getHasta() != null && filtro.getDesde().isAfter(filtro.getHasta())) {
            throw new ReglasDeNegocioException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        AlcanceInventarioDTO alcance = alcance(username);
        Sort orden = Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id"));
        return Paginacion.buscar(pagina, tam, orden, pageable -> movimientoRepositorio.findAll(
                        MovimientoEspecificaciones.filtrar(filtro, alcance.sucursalId()), pageable))
                .map(this::aFila);
    }
    // FB PASO 2 - CU-15 FINAL

    // FB PASO 2 - CU-15 INICIO
    @Transactional(readOnly = true)
    public MovimientoFilaDTO detalleMovimiento(Long id, String username) {
        return aFila(movimientoVisible(id, alcance(username)));
    }
    // FB PASO 2 - CU-15 FINAL

    // FA03 - CU-15 INICIO
    // RN-CU13-01: reglas que dependen del tipo elegido (las fijas van como anotaciones en el DTO)
    @Transactional(readOnly = true)
    public Map<String, String> validarCampos(MovimientoFormDTO form, String username) {
        Map<String, String> errores = new LinkedHashMap<>();
        AlcanceInventarioDTO alcance = alcance(username);
        if (alcance.administrador() && form.getSucursalId() == null) {
            errores.put("sucursalId", "Debe seleccionar una sucursal.");
        }
        TipoMovimiento tipo = form.getTipo();
        if (tipo == null) {
            return errores;
        }
        if (!tipo.isManual()) {
            errores.put("tipo", "Los movimientos de despacho los genera el sistema al despachar recetas.");
            return errores;
        }

        BigDecimal costo = tipo.isEntrada() ? form.getCostoUnitario() : null;
        if (tipo == TipoMovimiento.COMPRA && costo == null) {
            errores.put("costoUnitario", "El costo unitario es obligatorio para compras");
        } else if (costo != null && costo.compareTo(BigDecimal.ZERO) <= 0) {
            errores.put("costoUnitario", "El costo unitario debe ser mayor a 0");
        } else if (costo != null && costo.stripTrailingZeros().scale() > 2) {
            errores.put("costoUnitario", "El costo unitario debe tener máximo 2 decimales");
        }

        int largoMotivo = form.getMotivo() == null ? 0 : form.getMotivo().trim().length();
        if (tipo.isMotivoObligatorio() && (largoMotivo < 10 || largoMotivo > 500)) {
            errores.put("motivo", MENSAJE_MOTIVO);
        } else if (tipo.getEtiquetaMotivo() != null && largoMotivo > 500) {
            errores.put("motivo", MENSAJE_MOTIVO);
        }
        return errores;
    }
    // FA03 - CU-15 FINAL

    // FB PASOS 7-9 - CU-15 INICIO
    // Pasos 6 y 7 del CU: registra el movimiento y actualiza el stock en la misma transacción
    @Transactional
    public ResultadoMovimientoDTO registrar(MovimientoFormDTO form, String username) {
        AlcanceInventarioDTO alcance = alcance(username);
        Long sucursalId = alcance.administrador() ? form.getSucursalId() : alcance.sucursalId();
        if (!alcance.administrador() && form.getSucursalId() != null && !form.getSucursalId().equals(sucursalId)) {
            throw new ReglasDeNegocioException("Solo puede registrar movimientos de su sucursal.");
        }
        Medicamento medicamento = medicamentoRepositorio.findById(form.getMedicamentoId())
                .filter(Medicamento::getActivo)
                .orElseThrow(() -> new ReglasDeNegocioException("El medicamento seleccionado no existe o está inactivo."));
        Sucursal sucursal = sucursalRepositorio.findById(sucursalId)
                .filter(Sucursal::getActivo)
                .orElseThrow(() -> new ReglasDeNegocioException("La sucursal seleccionada no existe o está inactiva."));
        TipoMovimiento tipo = form.getTipo();
        int cantidad = form.getCantidad();

        InventarioMedicamento inventario = inventarioRepositorio
                .findByMedicamentoIdAndSucursalId(medicamento.getId(), sucursal.getId())
                .orElse(null);
        int anterior = inventario == null || !inventario.getActivo() ? 0 : inventario.getStockActual();

        // FA02 - CU-15 INICIO
        // FA02 / RN-CU13-02: una salida no puede dejar el stock en negativo
        if (!tipo.isEntrada() && cantidad > anterior) {
            throw new ReglasDeNegocioException("Stock insuficiente. Stock actual: " + anterior
                    + ". No se puede registrar una salida de " + cantidad + " unidades.");
        }
        // FA02 - CU-15 FINAL
        if (inventario == null) {
            inventario = InventarioMedicamento.builder()
                    .medicamento(medicamento)
                    .sucursal(sucursal)
                    .stockActual(0)
                    .activo(true)
                    .build();
        }
        // FB PASO 8 - CU-15 INICIO
        int nuevo = tipo.isEntrada() ? anterior + cantidad : anterior - cantidad;
        inventario.setStockActual(nuevo);
        inventario.setActivo(true);
        inventarioRepositorio.saveAndFlush(inventario);   // @Version: si otro lo cambió a la vez, falla aquí
        // FB PASO 8 - CU-15 FINAL

        MovimientoInventario movimiento = movimientoRepositorio.save(MovimientoInventario.builder()
                .tipo(tipo)
                .medicamento(medicamento)
                .sucursal(sucursal)
                .cantidad(cantidad)
                .stockAnterior(anterior)
                .stockNuevo(nuevo)
                .costoUnitario(tipo.isEntrada() ? form.getCostoUnitario() : null)
                .referencia(limpio(form.getReferencia()))
                .motivo(tipo.getEtiquetaMotivo() == null ? null : limpio(form.getMotivo()))
                .usuario(username)
                .fecha(LocalDateTime.now())
                .activo(true)
                .build());

        auditoriaService.registrar("MOV_INV_REGISTRADO", "MovimientoInventario", movimiento.getId(),
                tipo.getDescripcion() + " de " + cantidad + " " + medicamento.getNombre() + " en "
                        + sucursal.getNombre() + ": " + anterior + " -> " + nuevo);

        // FB PASO 9 - CU-15 INICIO
        String mensaje = "Movimiento registrado exitosamente. Medicamento: " + medicamento.getNombre()
                + ". Tipo: " + tipo.getDescripcion() + ". Cantidad: " + cantidad
                + ". Stock actualizado: " + nuevo + ".";
        // FB PASO 9 - CU-15 FINAL
        // FA05 - CU-15 INICIO
        int minimo = minimo(medicamento);
        String alerta = nuevo <= minimo
                ? medicamento.getNombre() + ": Stock bajo — disponible: " + nuevo + " (mínimo: " + minimo + ")"
                : null;
        // FA05 - CU-15 FINAL
        return new ResultadoMovimientoDTO(movimiento.getId(), mensaje, alerta);
    }
    // FB PASOS 7-9 - CU-15 FINAL

    // FB PASO 2 - CU-15 INICIO
    // Acción "Desactivar/Activar" de la tabla: solo cambia el estado del registro, nunca el stock
    @Transactional
    public String cambiarEstado(Long id, String username) {
        MovimientoInventario movimiento = movimientoVisible(id, alcance(username));
        if (!movimiento.getTipo().isManual()) {
            throw new ReglasDeNegocioException(
                    "Los movimientos de despacho los genera el sistema y no se pueden desactivar.");
        }
        boolean activo = !movimiento.getActivo();
        movimiento.setActivo(activo);
        movimientoRepositorio.saveAndFlush(movimiento);
        auditoriaService.registrar(activo ? "MOV_INV_ACTIVADO" : "MOV_INV_DESACTIVADO", "MovimientoInventario",
                movimiento.getId(), movimiento.getTipo().getDescripcion() + " de " + movimiento.getCantidad()
                        + " " + movimiento.getMedicamento().getNombre());
        return "Movimiento #" + movimiento.getId() + (activo ? " activado." : " desactivado.")
                + " El stock no cambia; para corregirlo registre un ajuste.";
    }
    // FB PASO 2 - CU-15 FINAL

    // ============================================================
    //  CATÁLOGOS PARA LOS FORMULARIOS
    // ============================================================

    @Transactional(readOnly = true)
    public List<Medicamento> medicamentosActivos() {
        return medicamentoRepositorio.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Sucursal> sucursalesActivas() {
        return sucursalRepositorio.findByActivoTrue();
    }

    // ============================================================
    //  APOYO
    // ============================================================

    private MovimientoInventario movimientoVisible(Long id, AlcanceInventarioDTO alcance) {
        return movimientoRepositorio.findOneById(id)
                .filter(m -> puedeVer(alcance, m.getSucursal().getId()))
                .orElseThrow(() -> new ReglasDeNegocioException("No se encontró el movimiento."));
    }

    private boolean puedeVer(AlcanceInventarioDTO alcance, Long sucursalId) {
        return alcance.administrador() || alcance.sucursalId().equals(sucursalId);
    }

    private int minimo(Medicamento medicamento) {
        return medicamento.getStockMinimo() == null ? 0 : medicamento.getStockMinimo();
    }

    private String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private InventarioFilaDTO aFila(InventarioMedicamento i) {
        int minimo = minimo(i.getMedicamento());
        return InventarioFilaDTO.builder()
                .id(i.getId())
                .medicamentoId(i.getMedicamento().getId())
                .medicamento(i.getMedicamento().getNombre())
                .unidad(i.getMedicamento().getUnidad())
                .controlado(Boolean.TRUE.equals(i.getMedicamento().getControlado()))
                .sucursalId(i.getSucursal().getId())
                .sucursal(i.getSucursal().getNombre())
                .stockActual(i.getStockActual())
                .stockMinimo(minimo)
                .bajo(i.getStockActual() <= minimo)
                .build();
    }

    private MovimientoFilaDTO aFila(MovimientoInventario m) {
        TipoMovimiento tipo = m.getTipo();
        return MovimientoFilaDTO.builder()
                .id(m.getId())
                .tipo(tipo.name())
                .tipoDescripcion(tipo.getDescripcion())
                .entrada(tipo.isEntrada())
                .medicamentoId(m.getMedicamento().getId())
                .medicamento(m.getMedicamento().getNombre())
                .sucursalId(m.getSucursal().getId())
                .sucursal(m.getSucursal().getNombre())
                .cantidad(m.getCantidad())
                .stockAnterior(m.getStockAnterior())
                .stockNuevo(m.getStockNuevo())
                .costoUnitario(m.getCostoUnitario())
                .etiquetaReferencia(tipo.getEtiquetaReferencia())
                .referencia(m.getReferencia())
                .etiquetaMotivo(tipo.getEtiquetaMotivo() == null ? "Motivo" : tipo.getEtiquetaMotivo())
                .motivo(m.getMotivo())
                .usuario(m.getUsuario())
                .fecha(m.getFecha())
                .activo(m.getActivo())
                .manual(tipo.isManual())
                .build();
    }
}
