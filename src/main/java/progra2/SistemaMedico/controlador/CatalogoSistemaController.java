package progra2.SistemaMedico.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import progra2.SistemaMedico.model.enums.AppointmentStatus;
import progra2.SistemaMedico.model.enums.UserRol;

@Controller
public class CatalogoSistemaController {

    // FB PASO 1 - CU-14 INICIO
    @GetMapping("/admin/catalogos")
    public String catalogos(Model model) {
        model.addAttribute("estadosCita", AppointmentStatus.values());
        model.addAttribute("roles", UserRol.values());
        return "admin/catalogos";
    }
    // FB PASO 1 - CU-14 FINAL
}
