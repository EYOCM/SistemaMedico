package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.MedicamentoFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.servicios.MedicamentoCatalogoService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/medicamentos")
public class MedicamentoCatalogoController {

    private final MedicamentoCatalogoService service;

    // FB PASO 2 - CU-14 INICIO
    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto,
                         @RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(defaultValue = "10") int tam,
                         Model model) {
        model.addAttribute("registros", service.listar(texto, pagina, tam));
        model.addAttribute("texto", texto);
        return "admin/medicamentos-lista";
    }
    // FB PASO 2 - CU-14 FINAL

    // FB PASOS 3-4 - CU-14 INICIO
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("registro", new MedicamentoFormDTO());
        return formulario(model);
    }
    // FB PASOS 3-4 - CU-14 FINAL

    // FA02 - CU-14 INICIO
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("registro", service.formulario(id));
            return formulario(model);
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/medicamentos";
        }
    }
    // FA02 - CU-14 FINAL

    // FB PASOS 6-8 / FA02 - CU-14 INICIO
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("registro") MedicamentoFormDTO form,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        // FA05 - CU-14 INICIO
        if (resultado.hasErrors()) {
            return formulario(model);
        }
        // FA05 - CU-14 FINAL
        try {
            redirectAttributes.addFlashAttribute("successMsg", service.guardar(form));
            return "redirect:/admin/medicamentos";
        } catch (ReglasDeNegocioException e) {
            // FA05 - CU-14 INICIO
            model.addAttribute("errorMsg", e.getMessage());
            return formulario(model);
            // FA05 - CU-14 FINAL
        }
    }
    // FB PASOS 6-8 / FA02 - CU-14 FINAL

    // FA03 - CU-14 INICIO
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMsg", service.eliminar(id));
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/medicamentos";
    }
    // FA03 - CU-14 FINAL

    private String formulario(Model model) {
        return "admin/medicamentos-form";
    }
}
