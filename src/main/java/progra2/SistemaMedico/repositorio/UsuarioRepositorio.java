package progra2.SistemaMedico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import progra2.SistemaMedico.model.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepositorio  extends JpaRepository<Usuario, Long> {
    /*interface que acuta como el acceso a los datos*/
    Optional<Usuario> findByUsername(String nombre) ;
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByDpi(String  dip);

    boolean existisByUsername (String username);
    boolean existByEmail (String email);
    boolean existsByDpi(String dpi);

}
