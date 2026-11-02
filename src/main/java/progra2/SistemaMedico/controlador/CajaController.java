package progra2.SistemaMedico.controlador;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.CobroCajaDTO;
import progra2.SistemaMedico.dto.ResultadoBusquedaCitaDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.enums.MetodoPago;
import progra2.SistemaMedico.servicios.CajaService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/caja")
public class CajaController {

    private final CajaService cajaService;

    // FB PASO 1 - CU-06 INICIO
    @GetMapping
    public String inicio() {
        return "caja/buscar";
    }
    // FB PASO 1 - CU-06 FINAL

    // FB PASO 1 - CU-06 INICIO
    @GetMapping("/buscar")
    public String buscar(@RequestParam(defaultValue = "numero") String tipo,
                         @RequestParam(required = false) String valor,
                         Model model) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("valor", valor);

        if (valor == null || valor.isBlank()) {
            model.addAttribute("error", "Debe ingresar un número de cita o DPI para buscar.");
            return "caja/buscar";
        }

        try {
            ResultadoBusquedaCitaDTO resultado = "dpi".equals(tipo)
                    ? cajaService.buscarPorDpi(valor)
                    : cajaService.buscarPorNumero(valor);
            model.addAttribute("resultado", resultado);
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());   // RN-GLOBAL-001
        }
        return "caja/buscar";
    }
    // FB PASO 1 - CU-06 FINAL

    // FB PASOS 2-4 - CU-06 INICIO
    @GetMapping("/citas/{citaId}/cobro")
    public String mostrarCobro(@PathVariable Long citaId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("cita", cajaService.obtenerCitaParaCobro(citaId));
            if (!model.containsAttribute("cobro")) {
                model.addAttribute("cobro", new CobroCajaDTO());
            }
            model.addAttribute("metodos", MetodoPago.enCaja());
            return "caja/cobro";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja";
        }
    }
    // FB PASOS 2-4 - CU-06 FINAL

    // FB PASOS 6-8 - CU-06 INICIO
    @PostMapping("/citas/{citaId}/cobro")
    public String registrarCobro(@PathVariable Long citaId,
                                 @ModelAttribute("cobro") CobroCajaDTO datos,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long pagoId = cajaService.registrarCobro(citaId, datos, principal.getName());
            redirectAttributes.addFlashAttribute("recienRegistrado", true);
            return "redirect:/caja/pagos/" + pagoId + "/comprobante";

        } catch (ReglasDeNegocioException e) {
            // FA04 - CU-06 INICIO
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("cobro", datos);
            return "redirect:/caja/citas/" + citaId + "/cobro";
            // FA04 - CU-06 FINAL

        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al registrar el pago. La cita fue actualizada por otro usuario; verifique su estado.");
            return "redirect:/caja";
        }
    }
    // FB PASOS 6-8 - CU-06 FINAL

    // FB PASOS 9-10 - CU-06 INICIO
    @GetMapping("/pagos/{pagoId}/comprobante")
    public String verComprobante(@PathVariable Long pagoId,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("comprobante", cajaService.obtenerComprobante(pagoId));
            return "caja/comprobante";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja";
        }
    }
    // FB PASOS 9-10 - CU-06 FINAL
}
