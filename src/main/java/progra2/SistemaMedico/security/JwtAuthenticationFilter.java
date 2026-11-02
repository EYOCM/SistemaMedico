package progra2.SistemaMedico.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { //OncePerRequestFilter extiende de esta clase abstracta para la personalizacion de fitlros

    private final GeneradorDeJwt jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.equals("/") ||
                requestURI.startsWith("/login") ||
                requestURI.startsWith("/registro") ||
                requestURI.startsWith("/dashboard") ||
                requestURI.startsWith("/admin/") ||
                requestURI.startsWith("/paciente/") ||
                requestURI.startsWith("/citas/") ||
                requestURI.startsWith("/pago/") ||
                requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/recepcion") ||
                requestURI.startsWith("/caja") ||
                requestURI.startsWith("/enfermeria") ||
                requestURI.startsWith("/medico") ||
                requestURI.startsWith("/laboratorio") ||
                requestURI.startsWith("/farmacia")
        )
        {


            // Continuar la cadena de filtros SIN aplicar lógica JWT
            filterChain.doFilter(request, response);
            return; // Salimos del método aquí para las vistas
        }

        // 2. APLICAR JWT SOLO A RUTAS DE API (/api/**)
        try {
            String jwt = parseJwt(request);
            if (jwt != null) {
                String username = jwtUtils.extractUsername(jwt);

                // Si hay un usuario y aún no está autenticado en el contexto de seguridad
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    // Validamos el token usando tu clase GeneradorDeJwt
                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
        }

        // 3. SIEMPRE continuar la cadena de filtros al final
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}