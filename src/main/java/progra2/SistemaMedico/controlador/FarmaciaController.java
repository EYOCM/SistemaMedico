package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.DespachoFormDTO;
import progra2.SistemaMedico.dto.DespachoLineaDTO;
import progra2.SistemaMedico.dto.DespachoResumenDTO;
import progra2.SistemaMedico.dto.PreparacionDespachoDTO;
import progra2.SistemaMedico.dto.RecetaBusquedaDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.servicios.FarmaciaService;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/farmacia")
public class FarmaciaController {

    private static final String MENSAJE_CONCURRENCIA =
            "El inventario o la receta fueron actualizados por otro usuario; intente de nuevo.";

    private final FarmaciaService farmaciaService;

    // FB PASO 1 - CU-11 INICIO
    @GetMapping
    public String inicio() {
        return "farmacia/buscar";
    }
    // FB PASO 1 - CU-11 FINAL

    // FB PASOS 2-3 - CU-11 INICIO
    @GetMapping("/buscar")
    public String buscar(@RequestParam(defaultValue = "receta") String tipo,
                         @RequestParam(required = false) String valor,
                         Model model) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("valor", valor);
        if (valor == null || valor.isBlank()) {
            model.addAttribute("error", "Ingrese un número de receta o de consulta para buscar.");
            return "farmacia/buscar";
        }
        List<RecetaBusquedaDTO> recetas = farmaciaService.buscar(tipo, valor);
        if (recetas.isEmpty()) {
            model.addAttribute("aviso", "No se encontraron recetas pendientes de despacho con ese criterio.");
        }
        model.addAttribute("recetas", recetas);
        return "farmacia/buscar";
    }
    // FB PASOS 2-3 - CU-11 FINAL

    // FB PASOS 4-6 - CU-11 INICIO
    @GetMapping("/recetas/{recetaId}/despacho")
    public String mostrarDespacho(@PathVariable Long recetaId, Principal principal, Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            PreparacionDespachoDTO prep = farmaciaService.preparar(recetaId, principal.getName());
            if (model.containsAttribute("despacho")) {
                copiarLoEscrito((DespachoFormDTO) model.getAttribute("despacho"), prep.getFormulario());
            }
            model.addAttribute("prep", prep);
            model.addAttribute("despacho", prep.getFormulario());
            return "farmacia/despacho";
        } catch (ReglasDeNegocioException e) {
            // FB PASO 4 - CU-11 INICIO
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/farmacia";
            // FB PASO 4 - CU-11 FINAL
        }
    }
    // FB PASOS 4-6 - CU-11 FINAL

    // FB PASOS 9-10 - CU-11 INICIO
    @PostMapping("/recetas/{recetaId}/despacho")
    public String confirmar(@PathVariable Long recetaId,
                            @Valid @ModelAttribute("despacho") DespachoFormDTO form,
                            BindingResult resultado,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return volverAlFormulario(recetaId, form, resultado.getAllErrors().get(0).getDefaultMessage(),
                    redirectAttributes);
        }
        try {
            Long despachoId = farmaciaService.confirmar(recetaId, form, principal.getName());
            redirectAttributes.addFlashAttribute("recienRegistrado", true);
            return "redirect:/farmacia/despachos/" + despachoId;
        } catch (ReglasDeNegocioException e) {
            return volverAlFormulario(recetaId, form, e.getMessage(), redirectAttributes);
        } catch (ObjectOptimisticLockingFailureException e) {
            return volverAlFormulario(recetaId, form, MENSAJE_CONCURRENCIA, redirectAttributes);
        }
    }
    // FB PASOS 9-10 - CU-11 FINAL

    // FA03 - CU-11 INICIO
    @PostMapping("/recetas/{recetaId}/no-adquirido")
    public String noAdquirido(@PathVariable Long recetaId, Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("success",
                    farmaciaService.registrarNoAdquirido(recetaId, principal.getName()));
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/farmacia";
    }
    // FA03 - CU-11 FINAL

    // FB PASOS 11-12 - CU-11 INICIO
    @GetMapping("/despachos/{despachoId}")
    public String resumen(@PathVariable Long despachoId, Model model, RedirectAttributes redirectAttributes) {
        try {
            DespachoResumenDTO resumen = farmaciaService.resumen(despachoId);
            model.addAttribute("resumen", resumen);
            return "farmacia/resumen";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/farmacia";
        }
    }
    // FB PASOS 11-12 - CU-11 FINAL

    private String volverAlFormulario(Long recetaId, DespachoFormDTO form, String mensaje,
                                      RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", mensaje);
        redirectAttributes.addFlashAttribute("despacho", form);
        return "redirect:/farmacia/recetas/" + recetaId + "/despacho";
    }

    private void copiarLoEscrito(DespachoFormDTO escrito, DespachoFormDTO destino) {
        for (DespachoLineaDTO linea : destino.getLineas()) {
            escrito.getLineas().stream()
                    .filter(e -> e != null && linea.getRecetaDetalleId().equals(e.getRecetaDetalleId()))
                    .findFirst()
                    .ifPresent(e -> {
                        linea.setCantidad(e.getCantidad());
                        linea.setSustituir(e.isSustituir());
                        linea.setMedicamentoSustitutoId(e.getMedicamentoSustitutoId());
                        linea.setRazonSustitucion(e.getRazonSustitucion());
                    });
        }
    }
}
