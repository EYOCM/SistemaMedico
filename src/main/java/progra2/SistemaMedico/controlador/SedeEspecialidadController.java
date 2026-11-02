package progra2.SistemaMedico.controlador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.SedeEspecialidadFilaDTO;
import progra2.SistemaMedico.dto.SedeEspecialidadFormDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.servicios.CatalogoService;
import progra2.SistemaMedico.servicios.SedeEspecialidadService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/sedes-especialidades")
public class SedeEspecialidadController {

    private final SedeEspecialidadService sedeEspecialidadService;
    private final CatalogoService catalogoService;

    // FB PASO 3 - CU-13 INICIO
    @GetMapping
    public String listar(@RequestParam(required = false) String id,
                         @RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(defaultValue = "10") int tam,
                         Model model) {
        Page<SedeEspecialidadFilaDTO> resultado;
        try {
            resultado = sedeEspecialidadService.listar(leerId(id), pagina, tam);
        } catch (NumberFormatException e) {
            model.addAttribute("errorMsg", "El ID debe ser un número entero.");
            resultado = Page.empty();
        }
        model.addAttribute("asignaciones", resultado);
        model.addAttribute("id", id);
        model.addAttribute("tam", resultado.getSize() > 0 ? resultado.getSize() : 10);
        return "admin/sedes-especialidades-lista";
    }
    // FB PASO 3 - CU-13 FINAL

    // FB PASOS 4-5 - CU-13 INICIO
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("asignacion", new SedeEspecialidadFormDTO());
        cargarCatalogos(model);
        return "admin/sedes-especialidades-form";
    }
    // FB PASOS 4-5 - CU-13 FINAL

    // FB PASOS 7-9 - CU-13 INICIO
    @PostMapping("/nueva")
    public String asignar(@Valid @ModelAttribute("asignacion") SedeEspecialidadFormDTO form,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        // FA03 - CU-13 INICIO
        if (resultado.hasErrors()) {
            return volverAlFormulario(model, resultado.getAllErrors().get(0).getDefaultMessage());
        }
        // FA03 - CU-13 FINAL
        try {
            redirectAttributes.addFlashAttribute("successMsg", sedeEspecialidadService.asignar(form));
            return "redirect:/admin/sedes-especialidades";
        } catch (ReglasDeNegocioException e) {
            // FA05 - CU-13 INICIO
            return volverAlFormulario(model, e.getMessage());
            // FA05 - CU-13 FINAL
        }
    }
    // FB PASOS 7-9 - CU-13 FINAL

    // FA02 - CU-13 INICIO
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMsg", sedeEspecialidadService.eliminar(id));
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/sedes-especialidades";
    }
    // FA02 - CU-13 FINAL

    private Long leerId(String id) {
        return id == null || id.isBlank() ? null : Long.parseLong(id.trim());
    }

    private String volverAlFormulario(Model model, String mensaje) {
        model.addAttribute("errorMsg", mensaje);
        cargarCatalogos(model);
        return "admin/sedes-especialidades-form";
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("sucursales", catalogoService.sucursalesActivas());
        model.addAttribute("especialidades", catalogoService.especiaidadesActivas());
    }
}
