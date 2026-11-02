package progra2.SistemaMedico.servicios;

import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.excepciones.ReglasDeNegocioException;
import progra2.SistemaMedico.model.Usuario;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;

import java.time.LocalDateTime;

@Service
@Data
public class GestionIntentosFallidos {
    private final UsuarioRepositorio repo;

    // FA06 - CU-00 INICIO
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resistrarYGuardarIntentosFallidos(Usuario usuario){
        Usuario usuarioDB = repo.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        int intentosActuales = usuarioDB.getIntentosFallidos() == null ? 0 : usuarioDB.getIntentosFallidos();
        int nuevosIntentos = intentosActuales + 1;

        usuarioDB.setIntentosFallidos(nuevosIntentos);
        usuarioDB.setFechaActualizacion(LocalDateTime.now());

        // FA07 - CU-00 INICIO
        if(nuevosIntentos >=5){
            usuarioDB.setFechaBloqueo(LocalDateTime.now().plusMinutes(15));
            System.out.println("Usuario bloqueado por 15 min");
        }
        // FA07 - CU-00 FINAL
        repo.save(usuarioDB);
        System.out.println("Intento " + nuevosIntentos + " guardado exitosamente en BD");
    }
    // FA06 - CU-00 FINAL

}
