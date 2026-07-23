package progra2.SistemaMedico.servicios;


import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import progra2.SistemaMedico.dto.UsuarioDTO;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;


import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepositorio usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true) // esta etiqueta indica una transaccion en la base de datos que sera unicamente de lectura
    public List<UsuarioDTO>listUsuarios(){
        return usuarioRepo.findAll().stream() //invoca al repo para traer los datos de la tabla y flitrarlos
                .map(this::convertirADTO) // con el map capta a todos los usuairos y hace referenica al metodo para convertirlos a DTO
                .collect(Collectors.toList());
        // .collect indica el fin del proceso, .collectors.toList toma los DTOS y los almacena en una nueval lista, esta lista es enviada a quien hace la consulta osea el controllador con los endpoints
    }

    @Transactional(readOnly = true)
    public UsuarioDTO

}
