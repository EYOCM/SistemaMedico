package progra2.SistemaMedico.controlador;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;
import progra2.SistemaMedico.security.CustomUserDetailsService;
import progra2.SistemaMedico.servicios.CitaService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CitaService citaService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Obtener el usuario autenticado
        String username = authentication.getName();
        model.addAttribute("username", username);

        return "dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        return "admin/dashboard";
    }

    // FB PASO 10 - CU-00 INICIO
    @GetMapping("/paciente/dashboard")
    public String pacienteDashboard(Model model) {
        return "paciente/dashboard";
    }
    // FB PASO 10 - CU-00 FINAL

    @GetMapping("/paciente/mis-citas")
    public String misCitas(Principal principal, Model model) {
        model.addAttribute("citas", citaService.misCitas(principal.getName()));
        return "paciente/mis-citas";
    }
}