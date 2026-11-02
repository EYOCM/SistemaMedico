package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.ResultadoExamenFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.enums.EstadoOrdenLaboratorio;
import progra2.SistemaMedico.servicios.LaboratorioService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/laboratorio")
public class LaboratorioController {

    private static final String MENSAJE_FUERA_DE_RANGO =
            "Los resultados están fuera del rango de referencia normal. Requiere revisión.";
    private static final String MENSAJE_CONCURRENCIA =
            "El resultado fue actualizado por otro usuario; verifique su estado.";

    private final LaboratorioService laboratorioService;

    // FB PASO 1 - CU-09 INICIO
    @GetMapping
    public String ordenes(@RequestParam(required = false) String estado,
                          @RequestParam(required = false) String paciente,
                          @RequestParam(required = false) String medico,
                          Model model) {
        model.addAttribute("ordenes", laboratorioService.listar(estado, paciente, medico));
        model.addAttribute("estados", EstadoOrdenLaboratorio.values());
        model.addAttribute("estado", estado);
        model.addAttribute("paciente", paciente);
        model.addAttribute("medico", medico);
        return "laboratorio/ordenes";
    }
    // FB PASO 1 - CU-09 FINAL

    // FB PASOS 2-3 - CU-09 INICIO
    @GetMapping("/ordenes/{ordenId}")
    public String detalle(@PathVariable Long ordenId, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("orden", laboratorioService.detalle(ordenId));
            return "laboratorio/detalle";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/laboratorio";
        }
    }
    // FB PASOS 2-3 - CU-09 FINAL

    // FB PASOS 9-10 - CU-09 INICIO
    @PostMapping("/ordenes/{ordenId}/examenes/{examenId}/resultado")
    public String guardarResultado(@PathVariable Long ordenId,
                                   @PathVariable Long examenId,
                                   @Valid @ModelAttribute ResultadoExamenFormDTO datos,
                                   BindingResult resultado,
                                   RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return volverConError(ordenId, examenId, datos,
                    resultado.getAllErrors().get(0).getDefaultMessage(), redirectAttributes);
        }
        try {
            boolean fueraDeRango = laboratorioService.guardarResultado(ordenId, examenId, datos);
            redirectAttributes.addFlashAttribute("success", "Resultado guardado exitosamente.");
            // FA02 - CU-09 INICIO
            if (fueraDeRango) {
                redirectAttributes.addFlashAttribute("advertencia", MENSAJE_FUERA_DE_RANGO);
            }
            // FA02 - CU-09 FINAL
        } catch (ReglasDeNegocioException e) {
            return volverConError(ordenId, examenId, datos, e.getMessage(), redirectAttributes);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", MENSAJE_CONCURRENCIA);
        }
        return "redirect:/laboratorio/ordenes/" + ordenId + "#examen-" + examenId;
    }
    // FB PASOS 9-10 - CU-09 FINAL

    // FB PASOS 11-14 - CU-09 INICIO
    @PostMapping("/ordenes/{ordenId}/examenes/{examenId}/publicar")
    public String publicar(@PathVariable Long ordenId,
                           @PathVariable Long examenId,
                           RedirectAttributes redirectAttributes) {
        try {
            boolean ordenCompleta = laboratorioService.publicar(ordenId, examenId);
            redirectAttributes.addFlashAttribute("success", "Resultado publicado exitosamente.");
            if (ordenCompleta) {
                redirectAttributes.addFlashAttribute("info",
                        "Todos los resultados fueron publicados. La orden quedó Completada.");
            }
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", MENSAJE_CONCURRENCIA);
        }
        return "redirect:/laboratorio/ordenes/" + ordenId + "#examen-" + examenId;
    }
    // FB PASOS 11-14 - CU-09 FINAL

    private String volverConError(Long ordenId, Long examenId, ResultadoExamenFormDTO datos,
                                  String mensaje, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", mensaje);
        redirectAttributes.addFlashAttribute("errorExamenId", examenId);
        redirectAttributes.addFlashAttribute("resultadoPrevio", datos);
        return "redirect:/laboratorio/ordenes/" + ordenId + "#examen-" + examenId;
    }
}
