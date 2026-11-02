
package progra2.SistemaMedico.controlador;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.ApiResponseDTO;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.dto.request.LoginRequest;
import progra2.SistemaMedico.dto.request.RespuestaLogeo;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.servicios.AutenticacionServicio;

import javax.swing.plaf.PanelUI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacionController {
    private final AutenticacionServicio autenticacion;
    private final AuthenticationManager authenticationManager;

/*    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<RespuestaLogeo>> login(@Valid @RequestBody LoginRequest solicitudLogueo) {
        System.out.println("Controller login peticion recibida para el usuario:" + solicitudLogueo.getUsername());
        RespuestaLogeo respuesta = autenticacion.login(solicitudLogueo);
        System.out.println("logeo exitoso, se genera respuesta");

        return ResponseEntity.ok(ApiResponseDTO.<RespuestaLogeo>builder()
                .success(true)
                .message("Autentiacion exitosa")
                .data(respuesta)
                .build());
    }*/

    /**
     * Endpoint para registrar un nuevo usuario externo (Paciente).
     * POST /api/auth/register
     */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponseDTO<RespuestaLogeo>> registrar(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        RespuestaLogeo respuesta = autenticacion.registrar(usuarioDTO);

        return ResponseEntity.ok(ApiResponseDTO.<RespuestaLogeo>builder()
                .success(true)
                .message("Usuario registrado con exito")
                .data(respuesta)
                .build());

    }

}
