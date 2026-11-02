package progra2.SistemaMedico.servicios;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.dto.RegistroDTO;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.dto.request.RespuestaLogeo;
import progra2.SistemaMedico.dto.request.LoginRequest;
import progra2.SistemaMedico.excepciones.CuentaBloqueadaException;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.mapper.UsuarioMapper;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;
import progra2.SistemaMedico.security.GeneradorDeJwt;
import progra2.SistemaMedico.servicios.serviceImpl.IAutenticacionServicio;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import progra2.SistemaMedico.servicios.serviceImpl.IEmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;


@Service

public class AutenticacionServicio implements IAutenticacionServicio {

    @Autowired
    private AuthenticationManager authenticationManager; // interface de spring security para Valida si usuario/contraseña son correctos.
    @Autowired
    private GeneradorDeJwt jwtGenerado;

    @Autowired
    private UsuarioRepositorio repo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private UsuarioMapper mapper;

    @Autowired
    private GestionIntentosFallidos gestionIntentosFallidos;

    @Autowired
    private IEmailService emailService;

    // FB PASO 9 - CU-00 INICIO
    @Override
    @Transactional
    public RespuestaLogeo login(LoginRequest solicitudDeLogueo) {
        System.out.println(" SERVICIO: Usuario encontrado, procediendo a generar token...");


        Usuario usuario = repo.findByUsername(solicitudDeLogueo.getUsername())
                .orElseThrow(() -> new ReglasDeNegocioException("Las credenciales ingresadas son incorrectas"));

        // FA07 - CU-00 INICIO
        // validcion de bloqueo por multiplesi intentos
        if (usuario.getFechaBloqueo() != null && usuario.getFechaBloqueo().isAfter(LocalDateTime.now())) {
            // ChronoUnit.MINUTES trunca hacia abajo: durante el ultimo minuto diria
            // "0 minutos". Contamos segundos y redondeamos hacia arriba.
            long segundos = ChronoUnit.SECONDS.between(LocalDateTime.now(), usuario.getFechaBloqueo());
            long minutosRestantes = (segundos + 59) / 60;
            throw new CuentaBloqueadaException(
                    "Cuenta bloqueada temporalmente. Intente de nuevo en " + minutosRestantes + " minuto(s).",
                    minutosRestantes);
// reinicio de intentos pasados los 15 min de bloqueo
        } else if (usuario.getFechaBloqueo() != null && !usuario.getFechaBloqueo().isAfter(LocalDateTime.now())) {
            System.out.println("El tiempo de bloqueo experio. se reinician los intentos fallidos");
            usuario.setIntentosFallidos(0);
            usuario.setFechaBloqueo(null);
            repo.save(usuario);
        }
        // FA07 - CU-00 FINAL
        //validar si el usuario esta activo
        if (!usuario.getActivo()) {
            throw new ReglasDeNegocioException("El usaurio se encuentra inactivo contacte al administrador");
        }
        try {
            // FB PASO 10 - CU-00 INICIO
            // 4. Intentar autenticar con Spring Security (valida la contraseña)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(solicitudDeLogueo.getUsername(), solicitudDeLogueo.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails details = (UserDetails) authentication.getPrincipal();

            // si el logueo es exitoso reinicia el conteo de errores
            usuario.setIntentosFallidos(0);
            usuario.setFechaBloqueo(null);
            usuario.setFechaActualizacion(LocalDateTime.now());
            repo.save(usuario);

            //generar token y devolver la respuesta
            String token = jwtGenerado.generateToken(details);
            return RespuestaLogeo.builder()
                    .token(token)
                    .tipoDeToken("Bearer")
                    .username(usuario.getUsername())
                    .usuarioId(usuario.getId())
                    .dpi(usuario.getDpi())
                    .nombre(usuario.getNombre())
                    .apellidos(usuario.getApellidos())
                    .esAdministrador(usuario.getRol() == UserRol.ADMINISTRADOR)
                    .rol(usuario.getRol())
                    .build();
                    // FB PASO 10 - CU-00 FINAL

        } catch (BadCredentialsException e) {
            // FA06 - CU-00 INICIO
            gestionIntentosFallidos.resistrarYGuardarIntentosFallidos(usuario);
            Usuario usuarioActualizado = repo.findById(usuario.getId()).orElseThrow();
            int intentosRestantes = 5 - usuarioActualizado.getIntentosFallidos();

            // FA07 - CU-00 INICIO
            if (usuarioActualizado.getIntentosFallidos() >= 5) {

                throw new ReglasDeNegocioException("Cuenta bloqueada temporalmente por múltiples intentos fallidos. Contacte al administrador del sistema o espere 15 minutos.");
                // FA07 - CU-00 FINAL
            } else {
                throw new ReglasDeNegocioException("Las credenciales ingresadas son incorrectas. Tiene " + intentosRestantes + " intentos restantes antes del bloqueo temporal.");
            }
            // FA06 - CU-00 FINAL
        }
    }
    // FB PASO 9 - CU-00 FINAL

    /**
     * Método para obtener usuario por username (usado en login web)
     */
    public Usuario obtenerPorUsername(String username) {
        return repo.findByUsername(username).orElse(null);
    }

    // FB PASO 8 - CU-00 INICIO
    /**
     * CU-00 paso 8: busca un usuario por su DPI.
     * Devuelve null si no existe ningun registro asociado a ese DPI.
     */
    public Usuario obtenerPorDpi(String dpi) {
        return repo.findByDpi(dpi).orElse(null);
    }
    // FB PASO 8 - CU-00 FINAL


    @Override
    @Transactional
    public RespuestaLogeo registrar(UsuarioDTO dto) {
        if (repo.existsByEmail(dto.getEmail())) {
            throw new ReglasDeNegocioException("El correo electronico ya esta registrado");
        }
        if (repo.existsByDpi(dto.getDpi())) {
            throw new ReglasDeNegocioException("El DPI ya esta registrado");
        }
        if (repo.existsByUsername(dto.getUsername())) {
            throw new ReglasDeNegocioException("El nombre de usuario ya esta registrado en la base de datos");
        }
        Usuario usuario = mapper.toEntity(dto);
        usuario.setPassword(encoder.encode(dto.getPassword()));
        usuario.setActivo(true);

        if (usuario.getRol() == null) {
            usuario.setRol(UserRol.PACIENTE);
        }

        Usuario usuarioGuaradado = repo.save(usuario);
        try {
            String nombreCompleto = usuarioGuaradado.getNombre() + " " + usuarioGuaradado.getApellidos();
            emailService.enviarCorreoBienvenida(usuarioGuaradado.getEmail(), nombreCompleto);
        } catch (Exception e) {
            // se lanza este log para no bloquear el registro del usuario en dado caso el servidor de correos falle
            System.err.println("El usuario fue registrado exitosamente, pero falló el envío del correo de bienvenida: " + e.getMessage());
        }

        Usuario usuarioGuardado = usuarioGuaradado;
// SE GENERA TOKEN PARA INICIO DE SESION AUTOMATICO
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername(usuarioGuardado.getUsername())
                .password(usuarioGuardado.getPassword())
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + usuarioGuardado.getRol().name()))
                .disabled(!usuarioGuardado.getActivo())
                .build();

        String token = jwtGenerado.generateToken(details);

        return RespuestaLogeo.builder()
                .token(token)
                .tipoDeToken("Bearer")
                .username(usuarioGuardado.getUsername())
                .usuarioId(usuarioGuardado.getId())
                .dpi(usuarioGuardado.getDpi())
                .nombre(usuarioGuardado.getNombre())
                .apellidos(usuarioGuardado.getApellidos())
                .esAdministrador(usuarioGuardado.getRol() == UserRol.ADMINISTRADOR)
                .rol(usuarioGuardado.getRol())
                .build();


    }


    // ============================================================
    // REGISTRO DE USUARIOS EXTERNOS
    // ============================================================

    // FA02 / FA03 - CU-02 INICIO
    @Transactional(readOnly = true)
    public Map<String, String> validarUnicidadRegistro(RegistroDTO dto) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (dto.getEmail() != null && repo.existsByEmail(dto.getEmail().trim())) {
            // FA03
            errores.put("email", "Ya existe una cuenta registrada con este correo electrónico.");
        }
        if (dto.getDpi() != null && repo.existsByDpi(dto.getDpi().trim())) {
            // FA02
            errores.put("dpi", "Ya existe una cuenta registrada con este número de DPI. Si ya tiene cuenta, inicie sesión.");
        }
        if (dto.getUsername() != null && repo.existsByUsername(dto.getUsername().trim())) {
            errores.put("username", "El nombre de usuario ya se encuentra registrado.");
        }
        return errores;
    }
    // FA02 / FA03 - CU-02 FINAL

    // FB PASOS 12-14 - CU-02 INICIO
    /**
     * registra al paciente y dispara el correo de bienvenida.
     */
    @Transactional
    public Usuario registrarPaciente(RegistroDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre().trim());
        usuario.setApellidos(dto.getApellidos().trim());
        usuario.setDpi(dto.getDpi().trim());
        usuario.setNit(dto.getNit().trim());
        usuario.setTelefono(dto.getTelefono().trim());
        usuario.setNumeroSeguro(vacioANull(dto.getNumeroSeguro()));
        usuario.setEmail(dto.getEmail().trim());
        usuario.setUsername(dto.getUsername().trim());
        usuario.setPassword(encoder.encode(dto.getPassword()));
        usuario.setRol(UserRol.PACIENTE);
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);

        Usuario guardado = repo.save(usuario);

        emailService.enviarCorreoBienvenida(
                guardado.getEmail(),
                guardado.getNombre() + " " + guardado.getApellidos());

        return guardado;
    }
    // FB PASOS 12-14 - CU-02 FINAL

    private String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}

