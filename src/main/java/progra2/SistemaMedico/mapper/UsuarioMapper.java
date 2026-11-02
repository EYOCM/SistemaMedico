package progra2.SistemaMedico.mapper;


import org.springframework.stereotype.Component;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.model.enums.UserRol;

// aca se crea una solciitud para crear un usuario y se convierte a entidad

@Component
public class UsuarioMapper {

    public UsuarioDTO convertirADTO(Usuario usuario) {
        if (usuario == null) return null;

        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setDpi(usuario.getDpi());
        dto.setNit(usuario.getNit());
        dto.setNombre(usuario.getNombre());
        dto.setApellidos(usuario.getApellidos());
        dto.setEmail(usuario.getEmail());
        dto.setTelefono(usuario.getTelefono());
        dto.setNumeroSeguro(usuario.getNumeroSeguro());   // faltaba: se perdia el dato
        dto.setEsActivo(usuario.getActivo());
        dto.setUsername(usuario.getUsername());
        dto.setRol(usuario.getRol());
        dto.setEsAdministrador(usuario.getRol() == UserRol.ADMINISTRADOR);


        return dto;

    }

    public Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;

        Usuario usuario = new Usuario();
        usuario.setId(dto.getId());
        usuario.setDpi(dto.getDpi());
        usuario.setNit(dto.getNit());
        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellidos());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());   // faltaba: se perdia el dato
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(dto.getPassword());
        usuario.setActivo(dto.getEsActivo());
        usuario.setRol(dto.getRol());

        return usuario;
    }


}
