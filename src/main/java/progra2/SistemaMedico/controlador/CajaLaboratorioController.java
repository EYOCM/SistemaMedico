package progra2.SistemaMedico.controlador;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.CobroCajaDTO;
import progra2.SistemaMedico.dto.OrdenCobroDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.enums.MetodoPago;
import progra2.SistemaMedico.servicios.CajaLaboratorioService;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/caja/laboratorio")
public class CajaLaboratorioController {

    private static final String MENSAJE_FA01 =
            "No se encontraron órdenes de laboratorio pendientes de pago. Verifique el DPI o número de orden e intente de nuevo.";

    private final CajaLaboratorioService cajaLaboratorioService;

    // FB PASO 1 - CU-10 INICIO
    @GetMapping
    public String inicio() {
        return "caja/laboratorio-buscar";
    }
    // FB PASO 1 - CU-10 FINAL

    // FB PASOS 2-3 - CU-10 INICIO
    @GetMapping("/buscar")
    public String buscar(@RequestParam(defaultValue = "numero") String tipo,
                         @RequestParam(required = false) String valor,
                         Model model) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("valor", valor);

        if (valor == null || valor.isBlank()) {
            model.addAttribute("error", "Debe ingresar un número de orden o DPI para buscar.");
            return "caja/laboratorio-buscar";
        }

        try {
            List<OrdenCobroDTO> ordenes = "dpi".equals(tipo)
                    ? cajaLaboratorioService.buscarPorDpi(valor)
                    : cajaLaboratorioService.buscarPorNumero(valor);
            // FA01 - CU-10 INICIO
            if (ordenes.isEmpty()) {
                model.addAttribute("aviso", MENSAJE_FA01);
            }
            // FA01 - CU-10 FINAL
            model.addAttribute("ordenes", ordenes);
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());   // RN-GLOBAL-001
        }
        return "caja/laboratorio-buscar";
    }
    // FB PASOS 2-3 - CU-10 FINAL

    // FB PASOS 4-5 - CU-10 INICIO
    @GetMapping("/ordenes/{ordenId}/cobro")
    public String mostrarCobro(@PathVariable Long ordenId, Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("orden", cajaLaboratorioService.obtenerOrdenParaCobro(ordenId));
            if (!model.containsAttribute("cobro")) {
                model.addAttribute("cobro", new CobroCajaDTO());
            }
            model.addAttribute("metodos", MetodoPago.enCaja());
            return "caja/laboratorio-cobro";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/laboratorio";
        }
    }
    // FB PASOS 4-5 - CU-10 FINAL

    // FB PASOS 6-9 - CU-10 INICIO
    @PostMapping("/ordenes/{ordenId}/cobro")
    public String registrarCobro(@PathVariable Long ordenId,
                                 @ModelAttribute("cobro") CobroCajaDTO datos,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long pagoId = cajaLaboratorioService.registrarCobro(ordenId, datos, principal.getName());
            redirectAttributes.addFlashAttribute("recienRegistrado", true);
            return "redirect:/caja/laboratorio/pagos/" + pagoId + "/comprobante";

        } catch (ReglasDeNegocioException e) {
            // FA04 - CU-10 INICIO
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("cobro", datos);
            return "redirect:/caja/laboratorio/ordenes/" + ordenId + "/cobro";
            // FA04 - CU-10 FINAL

        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al registrar el pago. La orden fue actualizada por otro usuario; verifique su estado.");
            return "redirect:/caja/laboratorio";
        }
    }
    // FB PASOS 6-9 - CU-10 FINAL

    // FB PASOS 10-11 - CU-10 INICIO
    @GetMapping("/pagos/{pagoId}/comprobante")
    public String verComprobante(@PathVariable Long pagoId, Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("comprobante", cajaLaboratorioService.obtenerComprobante(pagoId));
            return "caja/laboratorio-comprobante";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/laboratorio";
        }
    }
    // FB PASOS 10-11 - CU-10 FINAL
}
