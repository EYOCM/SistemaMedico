package progra2.SistemaMedico.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsuarioController {

    @GetMapping("/detalles_info")
    public String info(){
        return "detalles_info";
    }
}
