package progra2.SistemaMedico.servicios;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import progra2.SistemaMedico.model.Auditoria;
import progra2.SistemaMedico.repositorio.AuditoriaRepositorio;


@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepositorio repo;




    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String entidad, Long entidadId, String detalle) {
        try {
            repo.save(Auditoria.builder()
                    .accion(recortar(accion, 20))
                    .entidad(recortar(entidad, 50))
                    .entidadId(entidadId)
                    .ejecutadoPor(recortar(usuarioActual(), 50))
                    .detalle(recortar(detalle, 500))
                    .build());
        } catch (Exception e) {
            // La auditoria no romper la operacion de negocio.
            System.err.println("No se pudo escribir el log de auditoria: " + e.getMessage());
        }
    }

    private String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "sistema";
        }
        return auth.getName();
    }
}
