package progra2.SistemaMedico.controlador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.*;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.enums.TipoMovimiento;
import progra2.SistemaMedico.servicios.InventarioService;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inventario")
public class InventarioController {

    private static final String MENSAJE_CONCURRENCIA =
            "El inventario fue actualizado por otro usuario al mismo tiempo. Revise el stock e intente de nuevo.";
    private static final Map<String, String> MENSAJES_DE_TIPO = Map.of(
            "cantidad", "La cantidad debe ser un número entero positivo.",
            "costoUnitario", "El costo unitario debe ser un número válido.");

    private final InventarioService inventarioService;

    // ============================================================
    //  INVENTARIO
    // ============================================================

    // FB PASO 1 - CU-15 INICIO
    @GetMapping
    public String inventario(@RequestParam(required = false) String texto,
                             @RequestParam(defaultValue = "1") int pagina,
                             @RequestParam(defaultValue = "10") int tam,
                             Principal principal, Model model) {
        model.addAttribute("alcance", inventarioService.alcance(principal.getName()));
        model.addAttribute("texto", texto);
        model.addAttribute("registros", inventarioService.listarInventario(texto, principal.getName(), pagina, tam));
        return "inventario/lista";
    }
    // FB PASO 1 - CU-15 FINAL

    // FB PASO 1 - CU-15 INICIO
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Principal principal, Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("alcance", inventarioService.alcance(principal.getName()));
            model.addAttribute("detalle", inventarioService.detalleInventario(id, principal.getName()));
            return "inventario/detalle";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario";
        }
    }
    // FB PASO 1 - CU-15 FINAL

    // FB PASO 4 / FA05 - CU-15 INICIO
    @GetMapping("/stock")
    @ResponseBody
    public ResponseEntity<StockInfoDTO> stock(@RequestParam Long medicamentoId,
                                              @RequestParam(required = false) Long sucursalId,
                                              Principal principal) {
        try {
            return ResponseEntity.ok(inventarioService.stock(medicamentoId, sucursalId, principal.getName()));
        } catch (ReglasDeNegocioException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // FB PASO 4 / FA05 - CU-15 FINAL

    // ============================================================
    //  BITÁCORA DE MOVIMIENTOS
    // ============================================================

    // FB PASO 2 - CU-15 INICIO
    @GetMapping("/movimientos")
    public String movimientos(@ModelAttribute("filtro") FiltroMovimientoDTO filtro,
                              BindingResult resultado,
                              @RequestParam(defaultValue = "1") int pagina,
                              @RequestParam(defaultValue = "10") int tam,
                              Principal principal, Model model, HttpServletRequest request) {
        cargarCatalogos(principal.getName(), model);
        String consulta = request.getQueryString();
        model.addAttribute("volver", request.getRequestURI() + (consulta == null ? "" : "?" + consulta));
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Las fechas deben tener el formato dd/mm/aaaa.");
            filtro.setDesde(null);
            filtro.setHasta(null);
        }
        try {
            model.addAttribute("registros", inventarioService.buscarMovimientos(filtro, principal.getName(), pagina, tam));
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registros", Page.empty());
        }
        return "inventario/movimientos";
    }
    // FB PASO 2 - CU-15 FINAL

    // FB PASO 2 - CU-15 INICIO
    @GetMapping("/movimientos/{id}")
    public String movimiento(@PathVariable Long id, Principal principal, Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("alcance", inventarioService.alcance(principal.getName()));
            model.addAttribute("m", inventarioService.detalleMovimiento(id, principal.getName()));
            return "inventario/movimiento";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario/movimientos";
        }
    }
    // FB PASO 2 - CU-15 FINAL

    // FB PASO 2 - CU-15 INICIO
    @PostMapping("/movimientos/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam(required = false) String volver,
                                Principal principal, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("success", inventarioService.cambiarEstado(id, principal.getName()));
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        boolean seguro = volver != null && volver.startsWith("/inventario/") && !volver.contains("//");
        return "redirect:" + (seguro ? volver : "/inventario/movimientos");
    }
    // FB PASO 2 - CU-15 FINAL

    // ============================================================
    //  NUEVO MOVIMIENTO (pasos 2 a 7 del CU)
    // ============================================================

    // FB PASOS 3-4 - CU-15 INICIO
    @GetMapping("/movimientos/nuevo")
    public String nuevo(@RequestParam(required = false) Long medicamentoId,
                        @RequestParam(required = false) Long sucursalId,
                        Principal principal, Model model) {
        MovimientoFormDTO form = new MovimientoFormDTO();
        form.setMedicamentoId(medicamentoId);
        form.setSucursalId(sucursalId);
        model.addAttribute("movimiento", form);
        model.addAttribute("errores", Map.of());
        cargarCatalogos(principal.getName(), model);
        return "inventario/movimiento-form";
    }
    // FB PASOS 3-4 - CU-15 FINAL

    // FB PASOS 6-9 - CU-15 INICIO
    @PostMapping("/movimientos/nuevo")
    public String registrar(@Valid @ModelAttribute("movimiento") MovimientoFormDTO form,
                            BindingResult resultado,
                            Principal principal, Model model,
                            RedirectAttributes redirectAttributes) {
        // FA03 - CU-15 INICIO
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : resultado.getFieldErrors()) {
            String mensaje = error.isBindingFailure()
                    ? MENSAJES_DE_TIPO.getOrDefault(error.getField(), "El valor ingresado no es válido.")
                    : error.getDefaultMessage();
            errores.putIfAbsent(error.getField(), mensaje);
        }
        inventarioService.validarCampos(form, principal.getName()).forEach(errores::putIfAbsent);
        if (!errores.isEmpty()) {
            return volverAlFormulario(errores, null, principal, model);
        }
        // FA03 - CU-15 FINAL
        try {
            ResultadoMovimientoDTO r = inventarioService.registrar(form, principal.getName());
            // FB PASO 9 - CU-15 INICIO
            redirectAttributes.addFlashAttribute("success", r.mensaje());
            redirectAttributes.addFlashAttribute("alerta", r.alertaStockMinimo());
            return "redirect:/inventario/movimientos/" + r.movimientoId();
            // FB PASO 9 - CU-15 FINAL
        } catch (ReglasDeNegocioException e) {
            // FA02 - CU-15 INICIO
            return volverAlFormulario(errores, e.getMessage(), principal, model);
            // FA02 - CU-15 FINAL
        } catch (ObjectOptimisticLockingFailureException e) {
            return volverAlFormulario(errores, MENSAJE_CONCURRENCIA, principal, model);
        }
    }
    // FB PASOS 6-9 - CU-15 FINAL

    private String volverAlFormulario(Map<String, String> errores, String error, Principal principal, Model model) {
        model.addAttribute("errores", errores);
        model.addAttribute("error", error != null ? error : "Revise los campos marcados en rojo.");
        cargarCatalogos(principal.getName(), model);
        return "inventario/movimiento-form";
    }

    private void cargarCatalogos(String username, Model model) {
        model.addAttribute("alcance", inventarioService.alcance(username));
        model.addAttribute("medicamentos", inventarioService.medicamentosActivos());
        model.addAttribute("sucursales", inventarioService.sucursalesActivas());
        model.addAttribute("tipos", TipoMovimiento.values());
        model.addAttribute("tiposManuales", TipoMovimiento.manuales());
    }
}
