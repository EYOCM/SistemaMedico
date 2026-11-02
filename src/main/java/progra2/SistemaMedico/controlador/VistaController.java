package progra2.SistemaMedico.controlador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import progra2.SistemaMedico.dto.RegistroDTO;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.dto.UsuarioFormDTO;
import progra2.SistemaMedico.dto.request.LoginRequest;
import progra2.SistemaMedico.dto.request.RespuestaLogeo;
import progra2.SistemaMedico.excepciones.CuentaBloqueadaException;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.servicios.AutenticacionServicio;
import progra2.SistemaMedico.repositorio.EspecialidadRepositorio;
import progra2.SistemaMedico.repositorio.SucursalRepositorio;
import progra2.SistemaMedico.servicios.CatalogoService;
import progra2.SistemaMedico.servicios.UsuarioService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class VistaController {

    private final AutenticacionServicio autenticacionServicio;
    private final UsuarioService usuarioService;
    private final SecurityContextRepository securityContextRepository;
    private final CatalogoService catalogoService;



    // CU-00: Portal
    // FB PASOS 1-2 - CU-00 INICIO
    @GetMapping("/")
    public String portal(Model model) {
        // Agregar información del portal al modelo
        model.addAttribute("servicios", obtenerServicios());
        model.addAttribute("especialidades", obtenerEspecialidades());
        model.addAttribute("ubicaciones", obtenerUbicaciones());
        model.addAttribute("horarios", obtenerHorariosAtencion());
        return "portal";
    }

    private List<String> obtenerServicios() {
        return List.of(
                "Consulta Externa",
                "Emergencias 24/7",
                "Laboratorio Clínico",
                "Radiología e Imágenes",
                "Farmacia",
                "Hospitalización"
        );
    }

    private List<String> obtenerEspecialidades() {
        return List.of(
                "Medicina General",
                "Pediatría",
                "Cardiología",
                "Ginecología",
                "Traumatología",
                "Dermatología",
                "Oftalmología",
                "Neurología"
        );
    }

    private List<String> obtenerUbicaciones() {
        return List.of(
                "Sucursal Central - Zona 10",
                "Sucursal Norte - Zona 16",
                "Sucursal Sur - Zona 8"
        );
    }

    private List<String> obtenerHorariosAtencion() {
        return List.of(
                "Lunes a Viernes: 8:00 AM - 6:00 PM",
                "Sábados: 8:00 AM - 12:00 PM",
                "Emergencias: 24 horas todos los días"
        );
    }
    // FB PASOS 1-2 - CU-00 FINAL

    // FB PASO 9 - CU-00 INICIO
    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String registro,
                               @RequestParam(required = false) String dpi, Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Usuario o contraseña incorrectos. (Intentos restantes: verifique con admin)");
        }
        if (registro != null) {
            model.addAttribute("successMsg", "¡Registro exitoso! Por favor, inicie sesión.");
        }
        if (dpi != null) {
            model.addAttribute("prefillDpi", dpi);
        }
        model.addAttribute("panelAdmin", false);
        return "login";
    }
    // FB PASO 9 - CU-00 FINAL

    // FA09 - CU-00 INICIO
    /**
     * CU-00 FA09: punto de entrada del panel administrativo (personal del hospital).
     */
    @GetMapping("/admin/login")
    public String loginAdministrativo(Model model) {
        model.addAttribute("panelAdmin", true);
        return "login";
    }
    // FA09 - CU-00 FINAL

    // FB PASO 1 - CU-02 INICIO
    /** CU-02 paso 1: muestra el formulario de registro. */
    @GetMapping("/registro")
    public String mostrarRegistro(@RequestParam(required = false) String dpi, Model model) {
        RegistroDTO nuevoUsuario = new RegistroDTO();
        if (dpi != null) {
            nuevoUsuario.setDpi(dpi);
        }
        model.addAttribute("usuario", nuevoUsuario);
        return "registro";
    }
    // FB PASO 1 - CU-02 FINAL
    // FB PASOS 9-10 - CU-00 INICIO
    @PostMapping("/procesar-login")
    public String procesarLogin(@RequestParam String username,
                                @RequestParam String password,
                                @RequestParam(defaultValue = "portal") String origen,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String pantallaLogin = "admin".equals(origen) ? "redirect:/admin/login" : "redirect:/login";
        try {
            RespuestaLogeo respuesta = autenticacionServicio.login(new LoginRequest(username, password));

            // FA09 - CU-00 INICIO
            // FA09: el login del portal es exclusivo para pacientes; el personal usa el panel administrativo
            if (!"admin".equals(origen) && respuesta.getRol() != UserRol.PACIENTE) {
                SecurityContextHolder.clearContext();
                redirectAttributes.addFlashAttribute("errorMsg",
                        "Este acceso es exclusivo para pacientes. Si es personal del hospital, use el panel administrativo.");
                return "redirect:/login";
            }
            // FA09 - CU-00 FINAL

            session.setAttribute("usuarioLogueado", respuesta);
            session.setAttribute("token", respuesta.getToken());

            UserDetails userDetails = User.builder()
                    .username(respuesta.getUsername())
                    .password("")
                    .authorities(new SimpleGrantedAuthority("ROLE_" + respuesta.getRol().name()))
                    .build();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);  // ← persiste en la sesión

            // FB PASO 10 - CU-00 INICIO
            return switch (respuesta.getRol()){
                case ADMINISTRADOR -> "redirect:/admin/dashboard";
                case RECEPCIONISTA -> "redirect:/recepcion";
                case CAJERO -> "redirect:/caja";
                case ENFERMERO -> "redirect:/enfermeria";
                case MEDICO -> "redirect:/medico";
                case LABORATORISTA -> "redirect:/laboratorio";
                case FARMACEUTICO -> "redirect:/farmacia";
                default -> "redirect:/paciente/dashboard";
            };
            // FB PASO 10 - CU-00 FINAL
        }
        // FA07 - CU-00 INICIO
        catch (CuentaBloqueadaException e){
            redirectAttributes.addFlashAttribute("minutosBloqueo", e.getMinutosRestantes());
            return pantallaLogin;
        }
        // FA07 - CU-00 FINAL
        // FA06 - CU-00 INICIO
        catch (ReglasDeNegocioException e) {
            // FA06 / FA07: credenciales incorrectas o cuenta bloqueada
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return pantallaLogin;
            // FA06 - CU-00 FINAL
        } catch (Exception e) {
            // FA08 - CU-00 INICIO
            // FA08: cualquier otro fallo (conexion, usuario inexistente, etc.)
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg",
                    "No se pudo conectar con el servidor. Intente de nuevo mas tarde.");
            return pantallaLogin;
            // FA08 - CU-00 FINAL
        }
    }
    // FB PASOS 9-10 - CU-00 FINAL

    // FB PASOS 6-8 - CU-00 INICIO
    /**
     * CU-00 pasos 6, 7 y 8: verificacion del DPI desde el modal del portal.
     */
    @PostMapping("/verificar-dpi")
    public String verificarDpi(@RequestParam(required = false) String dpi,
                               RedirectAttributes redirectAttributes) {

        String valor = (dpi == null) ? "" : dpi.trim();

        // FA01 - CU-00 INICIO
        if (valor.isEmpty()) {
            redirectAttributes.addFlashAttribute("dpiError",
                    "El campo DPI es obligatorio. Por favor, ingrese su numero de DPI.");
            return "redirect:/";
        }
        if (!valor.matches("\\d+")) {
            redirectAttributes.addFlashAttribute("dpiError",
                    "El DPI debe contener unicamente numeros. No se permiten letras ni caracteres especiales.");
            return "redirect:/";
        }
        if (valor.length() != 13) {
            redirectAttributes.addFlashAttribute("dpiError",
                    "El DPI debe contener exactamente 13 digitos. Usted ingreso " + valor.length() + " digitos.");
            return "redirect:/";
        }
        // FA01 - CU-00 FINAL

        try {
            Usuario usuario = autenticacionServicio.obtenerPorDpi(valor);

            // FA03 - CU-00 INICIO
            // si no existe el DPI se redirije al formulario de registro
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("infoMsg",
                        "No se encontro un registro asociado a este DPI. Sera redirigido al formulario de registro.");
                return "redirect:/registro?dpi=" + valor;
            }
            // FA03 - CU-00 FINAL

            // FA04 - CU-00 INICIO
            // DPI de personal interno
            if (usuario.getRol() != UserRol.PACIENTE) {
                redirectAttributes.addFlashAttribute("dpiError",
                        "Este DPI pertenece a un usuario del sistema interno. Por favor, contacte a recepcion.");
                return "redirect:/";
            }
            // FA04 - CU-00 FINAL

            // FB PASO 8 - CU-00 INICIO
            //registrado como paciente, pantalla de inicio de sesion
            redirectAttributes.addFlashAttribute("successMsg",
                    "Bienvenido(a), " + usuario.getNombre() + ". Sera redirigido al formulario de agendamiento de cita.");
            return "redirect:/login?dpi=" + valor;
            // FB PASO 8 - CU-00 FINAL

        } catch (Exception e) {
            // FA05 - CU-00 INICIO
            //error de conexion con el servidor
            redirectAttributes.addFlashAttribute("dpiError",
                    "No se pudo conectar con el servidor. Intente de nuevo mas tarde.");
            return "redirect:/";
            // FA05 - CU-00 FINAL
        }
    }
    // FB PASOS 6-8 - CU-00 FINAL

    // FB PASOS 10-15 - CU-02 INICIO
    /**
     * CU-02 valida, registra y manda al login.

     */
    @PostMapping("/procesar-registro")
    public String procesarRegistro(@Valid @ModelAttribute("usuario") RegistroDTO dto,
                                   BindingResult resultado,
                                   RedirectAttributes redirectAttributes) {

        // FA04 - CU-02 INICIO
        // nombre completo
        if (dto.getNombre() != null && !dto.getNombre().isBlank()
                && dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
            int largo = (dto.getNombre().trim() + " " + dto.getApellidos().trim()).length();
            if (largo < 10 || largo > 100) {
                resultado.rejectValue("nombre", "nombre.longitud",
                        "El nombre debe contener entre 10 y 100 caracteres. Usted ingresó " + largo + " caracteres.");
            }
        }
        // FA04 - CU-02 FINAL

        // FA02 / FA03 - CU-02 INICIO
        // consulta a base de datos unicamente si el DPI ya tiene fomrato indicaco
        if (!resultado.hasErrors()) {
            Map<String, String> duplicados = autenticacionServicio.validarUnicidadRegistro(dto);
            duplicados.forEach((campo, mensaje) -> resultado.rejectValue(campo, "duplicado", mensaje));
        }
        // FA02 / FA03 - CU-02 FINAL

        // FA04 - CU-02 INICIO
        // si hay errores volvemos al formulario con los datos intactos
        if (resultado.hasErrors()) {
            return "registro";
        }
        // FA04 - CU-02 FINAL

        // FB PASOS 12-15 - CU-02 INICIO
        try {
            autenticacionServicio.registrarPaciente(dto);
            return "redirect:/login?registro=exitoso"; // paso 15
            // FB PASOS 12-15 - CU-02 FINAL
        } catch (ReglasDeNegocioException e) {
            resultado.reject("registro.fallido", e.getMessage());
            return "registro";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMsg",
                    "No se pudo completar el registro. Intente de nuevo más tarde.");
            return "redirect:/registro";
        }
    }
    // FB PASOS 10-15 - CU-02 FINAL

    // MANTENIMIENTO DE USUARIOS

    // FB PASOS 4-8 - CU-01 INICIO
    /**
     * pantalla "Listado de Usuarios".
     */
    @GetMapping("/admin/usuarios")
    public String listarUsuarios(@RequestParam(required = false) String filtro,
                                 @RequestParam(required = false) String valor,
                                 @RequestParam(defaultValue = "1") int pagina,
                                 @RequestParam(defaultValue = "10") int tam,
                                 Model model) {

        List<UsuarioDTO> resultados;
        try {
            resultados = usuarioService.buscarUsuarios(filtro, valor);
        } catch (ReglasDeNegocioException e) {
            // criterio de busqueda invalido
            model.addAttribute("errorMsg", e.getMessage());
            resultados = List.of();
        }

        // Paginacion en memoria
        if (tam != 10 && tam != 25 && tam != 50) {
            tam = 10;
        }
        int total = resultados.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tam));
        if (pagina < 1) {
            pagina = 1;
        }
        if (pagina > totalPaginas) {
            pagina = totalPaginas;
        }
        int desde = (pagina - 1) * tam;
        int hasta = Math.min(desde + tam, total);
        List<UsuarioDTO> visibles = (desde >= total) ? List.of() : resultados.subList(desde, hasta);

        model.addAttribute("usuarios", visibles);
        model.addAttribute("filtro", filtro);
        model.addAttribute("valor", valor);
        model.addAttribute("pagina", pagina);
        model.addAttribute("tam", tam);
        model.addAttribute("total", total);
        model.addAttribute("totalPaginas", totalPaginas);
        return "admin/usuarios-lista";
    }
    // FB PASOS 4-8 - CU-01 FINAL

    // FA01 - CU-01 INICIO
    /** CU-01 FA01: formulario de creacion. */
    @GetMapping("/admin/usuarios/nuevo")
    public String nuevoUsuario(Model model) {
        UsuarioFormDTO form = new UsuarioFormDTO();
        form.setActivo(true); // RN-CU01-10: por defecto Activo en creacion
        model.addAttribute("usuario", form);
        model.addAttribute("modo", "Crear");
        cargarCatalogos(model);
        return "admin/usuarios-form";
    }
    // FA01 - CU-01 FINAL

    // FA04 - CU-01 INICIO
    /** CU-01 FA04: formulario de edicion con los valores precargados. */
    @GetMapping("/admin/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable Long id, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("usuario", usuarioService.obtenerFormPorId(id));
            model.addAttribute("modo", "Editar");
            cargarCatalogos(model);
            return "admin/usuarios-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "No se encontro el usuario solicitado.");
            return "redirect:/admin/usuarios";
        }
    }
    // FA04 - CU-01 FINAL

    // FA01 / FA04 - CU-01 INICIO
    /**
     * CU-01: guarda el formulario, tanto en creacion como en edicion.
     */
    @PostMapping("/admin/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute("usuario") UsuarioFormDTO form,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        boolean esCreacion = (form.getId() == null);
        try {
            if (esCreacion) {
                usuarioService.crearUsuarioWeb(form);
                redirectAttributes.addFlashAttribute("successMsg", "Usuario creado correctamente.");
            } else {
                usuarioService.actualizarUsuarioWeb(form.getId(), form);
                redirectAttributes.addFlashAttribute("successMsg", "Usuario actualizado correctamente.");
            }
            return "redirect:/admin/usuarios";

        } catch (ReglasDeNegocioException e) {
            // FA06 - CU-01 INICIO
            // FA06: volvemos al formulario conservando lo que el usuario ya escribio
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("usuario", form);
            model.addAttribute("modo", esCreacion ? "Crear" : "Editar");
            cargarCatalogos(model);
            return "admin/usuarios-form";
            // FA06 - CU-01 FINAL
        }
    }
    // FA01 / FA04 - CU-01 FINAL

    // FA05 - CU-01 INICIO
    /** CU-01 FA05: eliminacion (borrado logico). */
    @PostMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            UsuarioFormDTO u = usuarioService.obtenerFormPorId(id);
            usuarioService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("successMsg",
                    "El usuario " + u.getUsername() + " ha sido eliminado correctamente.");
        } catch (ReglasDeNegocioException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "No se pudo eliminar el usuario.");
        }
        return "redirect:/admin/usuarios";
    }
    // FA05 - CU-01 FINAL

    /** Catalogos que alimentan los desplegables del formulario. */
    private void cargarCatalogos(Model model) {
        model.addAttribute("roles", UserRol.values());
        model.addAttribute("sucursales", catalogoService.sucursalesActivas());
        model.addAttribute("especialidades", catalogoService.especiaidadesActivas());
    }
}
