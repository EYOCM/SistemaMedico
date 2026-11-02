package progra2.SistemaMedico.servicios;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.dto.UsuarioFormDTO;
import progra2.SistemaMedico.dto.UsuarioUpdateDTO;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.excepciones.ResourceNotFoundException;
import progra2.SistemaMedico.mapper.UsuarioMapper;
import progra2.SistemaMedico.model.Especialidad;
import progra2.SistemaMedico.model.Sucursal;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.UserRol;
import progra2.SistemaMedico.repositorio.EspecialidadRepositorio;
import progra2.SistemaMedico.repositorio.SucursalRepositorio;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;
import progra2.SistemaMedico.servicios.serviceImpl.IUsuarioService;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService implements IUsuarioService {
    @Autowired
    private UsuarioRepositorio repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Autowired
    private SucursalRepositorio sucursalRepositorio;

    @Autowired
    private EspecialidadRepositorio especialidadRepositorio;

    @Autowired
    private AuditoriaService auditoriaService;


    // ============================================================
    //  VALIDACIONES REUTILIZABLES
    // ============================================================

    private void validarDpi(String dpi) {
        if (dpi == null || dpi.isBlank()) {
            throw new ReglasDeNegocioException("El campo DPI es obligatorio. Por favor, ingrese su número de DPI.");
        }
        validarFormatoDpi(dpi);
    }

    /** Solo el FORMATO. Se usa cuando el DPI es opcional pero viene informado. */
    private void validarFormatoDpi(String dpi) {
        if (!dpi.matches("\\d+")) {
            throw new ReglasDeNegocioException("El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.");
        }
        if (dpi.length() != 13) {
            throw new ReglasDeNegocioException("El DPI debe contener exactamente 13 dígitos. Usted ingresó [" + dpi.length() + "] dígitos.");
        }
    }

    private void validarNit(String nit) {
        if (nit == null || nit.isBlank()) {
            throw new ReglasDeNegocioException("El campo NIT es obligatorio.");
        }
        validarFormatoNit(nit);
    }

    /** Solo el FORMATO. Se usa cuando el NIT es opcional pero viene informado. */
    private void validarFormatoNit(String nit) {
        if (!nit.matches("[a-zA-Z0-9]+")) {
            throw new ReglasDeNegocioException("El NIT debe contener únicamente caracteres alfanuméricos.");
        }
        if (nit.length() < 8 || nit.length() > 9) {
            throw new ReglasDeNegocioException("El NIT debe contener entre 8 y 9 caracteres. Usted ingresó [" + nit.length() + "] caracteres.");
        }
    }

    //  METODOS DE LA INTERFAZ
    // ============================================================

    @Override
    public List<UsuarioDTO> listarUsuarios() {
        return repo.findByActivo(true).stream()
                .map(usuarioMapper::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> obtenerPorId(Long id) {
        return repo.findById(id).map(usuarioMapper::convertirADTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioDTO> obtenerPorDpi(String dpi) {
        return repo.findByDpi(dpi).map(usuarioMapper::convertirADTO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO) {

        validarDpi(usuarioDTO.getDpi());
        validarNit(usuarioDTO.getNit());

        if (repo.existsByDpi(usuarioDTO.getDpi())) {
            throw new ReglasDeNegocioException("Ya existe un usuario con este numero de DPI" + usuarioDTO.getDpi());
        }
        if (repo.existsByEmail(usuarioDTO.getEmail())) {
            throw new ReglasDeNegocioException("Ya existe un usuario registrado con este Email");
        }
        if (repo.existsByUsername(usuarioDTO.getUsername())) {
            throw new ReglasDeNegocioException("El nombre de usuario " + usuarioDTO.getUsername()
                    + " ya se encuentra registrado. Por favor, elija otro.");
        }

        Usuario nuevoUsuario = usuarioMapper.toEntity(usuarioDTO);
        nuevoUsuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        // si el adnimistrador envia un rol lo guarda si no por defecto le asigna el de pacinete
        nuevoUsuario.setRol(usuarioDTO.getRol() != null ? usuarioDTO.getRol() : UserRol.PACIENTE);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setFechaCreacion(LocalDateTime.now());

        Usuario guardado = repo.save(nuevoUsuario);
        return usuarioMapper.convertirADTO(guardado);
    }

    @Override
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UsuarioUpdateDTO dtoActualizado) {
        Usuario existe = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        if (dtoActualizado.getNombre() != null) {
            existe.setNombre(dtoActualizado.getNombre());
        }
        if (dtoActualizado.getApellidos() != null) {
            existe.setApellidos(dtoActualizado.getApellidos());
        }
        if (dtoActualizado.getEmail() != null) {
            existe.setEmail(dtoActualizado.getEmail());
        }
        if (dtoActualizado.getTelefono() != null) {
            existe.setTelefono(dtoActualizado.getTelefono());
        }
        if (dtoActualizado.getPassword() != null && !dtoActualizado.getPassword().isEmpty()) {
            existe.setPassword(passwordEncoder.encode(dtoActualizado.getPassword()));
        }
        if (dtoActualizado.getRol() != null) {
            existe.setRol(dtoActualizado.getRol());
        }
        if (dtoActualizado.getEsActivo() != null) {
            existe.setActivo(dtoActualizado.getEsActivo());
        }

        Usuario actualizado = repo.save(existe);
        return usuarioMapper.convertirADTO(actualizado);
    }

    // FA05 - CU-01 INICIO
    @Override
    public void eliminarUsuario(Long id) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuario", "id", id));
        if(!usuario.getActivo()){
            throw new ReglasDeNegocioException("El usuario ya se encuentra inactivo o eliminado");
        }
        usuario.setActivo(false); // cambio de estado a falso(borrado logico)
        usuario.setFechaActualizacion(LocalDateTime.now()); // actualizacion de fecha de modificacion
        repo.save(usuario);

        auditoriaService.registrar("ELIMINAR", "Usuario", usuario.getId(),
                "Se desactivó (borrado lógico) al usuario " + usuario.getUsername());
    }
    // FA05 - CU-01 FINAL



    //  MANTENIMIENTO DE USUARIOS
    // ============================================================

    // FB PASOS 6-7 - CU-01 INICIO
    @Transactional(readOnly = true)
    public List<UsuarioDTO> buscarUsuarios(String filtro, String valor) {

        List<UsuarioDTO> todos = repo.findAll().stream()
                .map(usuarioMapper::convertirADTO)
                .collect(Collectors.toList());

        // FA02 - CU-01 INICIO
        if (valor == null || valor.isBlank() || filtro == null || filtro.isBlank()) {
            return todos; // limpiar búsqueda
        }
        // FA02 - CU-01 FINAL

        // RN-CU01-01: el campo de búsqueda no puede exceder 25 caracteres
        String criterio = valor.trim().toLowerCase();

        if (criterio.length() > 25) {
            throw new ReglasDeNegocioException("El campo de búsqueda no puede exceder los 25 caracteres.");
        }

        return todos.stream()
                .filter(u -> coincide(u, filtro, criterio))
                .collect(Collectors.toList());
    }

    private boolean coincide(UsuarioDTO u, String filtro, String criterio) {
        switch (filtro) {
            case "id":
                return u.getId() != null && u.getId().toString().equals(criterio);
            case "nombre":
                String completo = ((u.getNombre() == null ? "" : u.getNombre()) + " "
                        + (u.getApellidos() == null ? "" : u.getApellidos())).toLowerCase();
                return completo.contains(criterio);
            case "email":
                return u.getEmail() != null && u.getEmail().toLowerCase().contains(criterio);
            case "rol":
                return u.getRol() != null && u.getRol().name().toLowerCase().contains(criterio);
            case "username":
                return u.getUsername() != null && u.getUsername().toLowerCase().contains(criterio);
            case "dpi":
                return u.getDpi() != null && u.getDpi().contains(criterio);
            default:
                return true;
        }
    }
    // FB PASOS 6-7 - CU-01 FINAL

    // FA04 - CU-01 INICIO
    @Transactional(readOnly = true)
    public UsuarioFormDTO obtenerFormPorId(Long id) {
        Usuario u = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        UsuarioFormDTO form = new UsuarioFormDTO();
        form.setId(u.getId());
        form.setNombre(u.getNombre());
        form.setApellidos(u.getApellidos());
        form.setUsername(u.getUsername());
        form.setEmail(u.getEmail());
        form.setDpi(u.getDpi());
        form.setNit(u.getNit());
        form.setTelefono(u.getTelefono());
        form.setNumeroSeguro(u.getNumeroSeguro());
        form.setRol(u.getRol());
        form.setActivo(u.getActivo());
        form.setSucursalId(u.getSucursal() != null ? u.getSucursal().getId() : null);
        form.setEspecialidadId(u.getEspecialidad() != null ? u.getEspecialidad().getId() : null);
        // La contraseña NUNCA se devuelve al formulario: se deja vacía y solo
        // se cambia si el administrador escribe una nueva.
        return form;
    }
    // FA04 - CU-01 FINAL

    // FA01 - CU-01 INICIO
    /** FA01: crear usuario desde el formulario web. */
    @Transactional
    public Usuario crearUsuarioWeb(UsuarioFormDTO form) {
        validarFormulario(form, true);

        Usuario u = new Usuario();
        aplicarFormulario(u, form);
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setFechaCreacion(LocalDateTime.now());

        Usuario guardado = repo.save(u);
        auditoriaService.registrar("CREAR", "Usuario", guardado.getId(),
                "Se creó el usuario " + guardado.getUsername() + " con rol " + guardado.getRol());
        return guardado;
    }
    // FA01 - CU-01 FINAL

    // FA04 - CU-01 INICIO
    /** FA04: actualizar usuario desde el formulario web. */
    @Transactional
    public Usuario actualizarUsuarioWeb(Long id, UsuarioFormDTO form) {
        Usuario u = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));

        form.setId(id);
        validarFormulario(form, false);

        aplicarFormulario(u, form);
        // La contraseña solo se toca si el administrador escribió una nueva.
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        u.setFechaActualizacion(LocalDateTime.now());

        Usuario guardado = repo.save(u);
        auditoriaService.registrar("ACTUALIZAR", "Usuario", guardado.getId(),
                "Se actualizó el usuario " + guardado.getUsername());
        return guardado;
    }
    // FA04 - CU-01 FINAL

    private void aplicarFormulario(Usuario u, UsuarioFormDTO form) {
        u.setNombre(form.getNombre().trim());
        u.setApellidos(form.getApellidos().trim());
        u.setUsername(form.getUsername().trim());
        u.setEmail(form.getEmail().trim());
        u.setDpi(vacioANull(form.getDpi()));
        u.setNit(vacioANull(form.getNit()));
        u.setTelefono(vacioANull(form.getTelefono()));
        u.setNumeroSeguro(vacioANull(form.getNumeroSeguro()));
        u.setRol(form.getRol());
        u.setActivo(form.getActivo());

        if (form.getSucursalId() != null) {
            Sucursal s = sucursalRepositorio.findById(form.getSucursalId())
                    .orElseThrow(() -> new ReglasDeNegocioException("La sucursal seleccionada no existe."));
            u.setSucursal(s);
        } else {
            u.setSucursal(null);
        }

        // La especialidad solo para medicos
        if (form.getRol() == UserRol.MEDICO && form.getEspecialidadId() != null) {
            Especialidad e = especialidadRepositorio.findById(form.getEspecialidadId())
                    .orElseThrow(() -> new ReglasDeNegocioException("La especialidad seleccionada no existe."));
            u.setEspecialidad(e);
        } else {
            u.setEspecialidad(null);
        }
    }

    private String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // FA06 - CU-01 INICIO
    private void validarFormulario(UsuarioFormDTO form, boolean esCreacion) {

        Long idActual = form.getId();

        if (form.getNombre() == null || form.getNombre().isBlank()) {
            throw new ReglasDeNegocioException("El campo Nombre es obligatorio.");
        }
        if (form.getApellidos() == null || form.getApellidos().isBlank()) {
            throw new ReglasDeNegocioException("El campo Apellidos es obligatorio.");
        }
        String nombreCompleto = (form.getNombre().trim() + " " + form.getApellidos().trim()).trim();
        if (nombreCompleto.length() < 10 || nombreCompleto.length() > 100) {
            throw new ReglasDeNegocioException("El nombre debe contener entre 10 y 100 caracteres. Usted ingresó "
                    + nombreCompleto.length() + " caracteres.");
        }

        if (form.getUsername() == null || form.getUsername().isBlank()) {
            throw new ReglasDeNegocioException("El campo Usuario es obligatorio.");
        }
        String username = form.getUsername().trim();
        if (username.length() > 9) {
            throw new ReglasDeNegocioException("El usuario no puede exceder los 9 caracteres.");
        }
        if (username.length() < 8) {
            throw new ReglasDeNegocioException("El usuario debe contener al menos 8 caracteres.");
        }
        if (!username.matches("[a-zA-Z0-9]+")) {
            throw new ReglasDeNegocioException("El usuario debe contener únicamente caracteres alfanuméricos.");
        }
        repo.findByUsername(username)
                .filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new ReglasDeNegocioException("El nombre de usuario " + username
                            + " ya se encuentra registrado. Por favor, elija otro.");
                });

        boolean cambiaPassword = form.getPassword() != null && !form.getPassword().isBlank();
        if (esCreacion && !cambiaPassword) {
            throw new ReglasDeNegocioException("El campo Contraseña es obligatorio.");
        }
        if (cambiaPassword && form.getPassword().length() < 12) {
            throw new ReglasDeNegocioException("La contraseña debe contener al menos 12 caracteres.");
        }

        if (form.getEmail() == null || form.getEmail().isBlank()) {
            throw new ReglasDeNegocioException("El campo Correo Electrónico es obligatorio.");
        }
        String email = form.getEmail().trim();
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            throw new ReglasDeNegocioException("El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com");
        }
        repo.findByEmail(email)
                .filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new ReglasDeNegocioException("El correo electrónico ya se encuentra registrado.");
                });

        String dpi = vacioANull(form.getDpi());
        if (dpi != null) {
            validarFormatoDpi(dpi);
            repo.findByDpi(dpi)
                    .filter(otro -> !otro.getId().equals(idActual))
                    .ifPresent(otro -> {
                        throw new ReglasDeNegocioException("Ya existe un usuario registrado con el DPI " + dpi + ".");
                    });
        }

        String nit = vacioANull(form.getNit());
        if (nit != null) {
            validarFormatoNit(nit);
            repo.findByNit(nit)
                    .filter(otro -> !otro.getId().equals(idActual))
                    .ifPresent(otro -> {
                        throw new ReglasDeNegocioException("Ya existe un usuario registrado con el NIT " + nit + ".");
                    });
        }

        String telefono = vacioANull(form.getTelefono());
        if (telefono != null && !telefono.matches("\\d{8}")) {
            throw new ReglasDeNegocioException("El teléfono debe contener exactamente 8 dígitos.");
        }

        String seguro = vacioANull(form.getNumeroSeguro());
        if (seguro != null && (seguro.length() < 5 || seguro.length() > 50)) {
            throw new ReglasDeNegocioException("El número de seguro debe contener entre 5 y 50 caracteres.");
        }

        if (form.getRol() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar un rol para el usuario.");
        }

        if (form.getActivo() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar un estado para el usuario.");
        }

        if (esCreacion && form.getSucursalId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una sucursal para el usuario.");
        }

        if (form.getRol() == UserRol.MEDICO && form.getEspecialidadId() == null) {
            throw new ReglasDeNegocioException("Debe seleccionar una especialidad para el médico.");
        }
    }
    // FA06 - CU-01 FINAL
}
