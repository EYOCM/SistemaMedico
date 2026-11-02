package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.CitaDTO;
import progra2.SistemaMedico.dto.SignosVitalesDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.servicios.EnfermeriaService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/enfermeria")
public class EnfermeriaController {

    private final EnfermeriaService enfermeriaService;

    // FB PASO 1 - CU-07 INICIO
    @GetMapping
    public String panel(Principal principal, Model model) {
        try {
            model.addAttribute("panel", enfermeriaService.obtenerPanel(principal.getName()));
        } catch (ReglasDeNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("panel", null);
        }
        return "enfermeria/panel";
    }
    // FB PASO 1 - CU-07 FINAL

    // FB PASO 1 - CU-07 INICIO
    @PostMapping("/citas/{citaId}/llamar")
    public String llamar(@PathVariable Long citaId,
                         @RequestParam int turno,
                         RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = enfermeriaService.llamar(citaId);
            redirectAttributes.addFlashAttribute("anuncio",
                    "Turno número " + turno + ". Paciente " + cita.getNombrePaciente()
                            + ", favor pasar a toma de signos vitales.");
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error", "Este paciente ya fue llamado por otra enfermera.");
        }
        return "redirect:/enfermeria";
    }
    // FB PASO 1 - CU-07 FINAL

    /// //////////////////////////////////////////////////////////////////////////////////////////////
    //CU7
    // FB PASOS 2-3 - CU-07 INICIO
    @GetMapping("/citas/{citaId}/signos")
    public String mostrarFormulario(@PathVariable Long citaId,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            CitaDTO cita = enfermeriaService.obtenerCitaParaSignos(citaId);
            model.addAttribute("cita", cita);
            SignosVitalesDTO signos = new SignosVitalesDTO();
            // FA01 - CU-07 INICIO
            signos.setEmergencia("EMERGENCIA".equals(cita.getPrioridad()));
            // FA01 - CU-07 FINAL
            model.addAttribute("signos", signos);
            return "enfermeria/signos";
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/enfermeria";
        }
    }
    // FB PASOS 2-3 - CU-07 FINAL

    // FB PASOS 9-12 - CU-07 INICIO
    @PostMapping("/citas/{citaId}/signos")
    public String registrar(@PathVariable Long citaId,
                            @Valid @ModelAttribute("signos") SignosVitalesDTO datos,
                            BindingResult resultado,
                            Principal principal,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        // FA02 - CU-07 INICIO
        if (resultado.hasErrors()) {
            try {
                model.addAttribute("cita", enfermeriaService.obtenerCitaParaSignos(citaId));
                return "enfermeria/signos";
            } catch (ReglasDeNegocioException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/enfermeria";
            }
        }
        // FA02 - CU-07 FINAL

        try {
            CitaDTO cita = enfermeriaService.registrar(citaId, datos, principal.getName());
            String mensaje;
            // FA01 - CU-07 INICIO
            if ("EMERGENCIA".equals(cita.getPrioridad())) {
                mensaje = "Signos vitales de emergencia registrados para paciente " + cita.getNombrePaciente()
                        + ". El paciente debe pasar directamente a consulta médica.";
            // FA01 - CU-07 FINAL
            } else {
                // FB PASO 12 - CU-07 INICIO
                mensaje = "Signos vitales del paciente " + cita.getNombrePaciente()
                        + " registrados correctamente. El paciente puede regresar a la sala de espera.";
                // FB PASO 12 - CU-07 FINAL
            }
            redirectAttributes.addFlashAttribute("success", mensaje);
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al registrar los signos vitales. La cita fue actualizada por otro usuario; verifique su estado.");
        }
        return "redirect:/enfermeria";
    }
    // FB PASOS 9-12 - CU-07 FINAL

}