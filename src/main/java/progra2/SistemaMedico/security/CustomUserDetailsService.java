package progra2.SistemaMedico.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import progra2.SistemaMedico.repositorio.UsuarioRepositorio;

// sevicio para cargar detalles del usuario desde DB
    @Service
    @RequiredArgsConstructor

    /*esta clase implementa de la interface UserDetailService que es la interface se seguridad de spring de
     la cual solo necesita saber contrase;a cifrada, permisos y si la cuenta ya esta habilitda */
    public class CustomUserDetailsService implements UserDetailsService {

        private final UsuarioRepositorio repo;
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("[Buscando] usuario con username: " + username);

        return repo.findByUsername(username)
                .map(usuario -> {
                    // NUNCA imprimir la contrasena (ni el hash) en consola.
                    System.out.println("Usuario encontrado: " + usuario.getUsername()
                            + " | activo=" + usuario.getActivo()
                            + " | rol=" + usuario.getRol());

                    return org.springframework.security.core.userdetails.User
                            .withUsername(usuario.getUsername())
                            .password(usuario.getPassword())
                            .roles(usuario.getRol().name())
                            .disabled(!usuario.getActivo())
                            .build();
                })
                .orElseThrow(() -> {
                    System.err.println("[Usuario NO encontrado:] " + username);
                    return new UsernameNotFoundException("[Usuario no encontrado:] " + username);
                });
    }
    }

