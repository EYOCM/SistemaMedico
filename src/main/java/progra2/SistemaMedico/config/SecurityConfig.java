package progra2.SistemaMedico.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import progra2.SistemaMedico.security.JwtAuthenticationFilter;


import java.util.Arrays;
import java.util.List;

        @Configuration
        @EnableWebSecurity
        @EnableMethodSecurity
        @RequiredArgsConstructor
        public class SecurityConfig {

            private final JwtAuthenticationFilter jwtAuthenticationFilter;
            private final UserDetailsService userDetailsService;
            private final PasswordEncoder passwordEncoder;

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(csrf -> csrf.disable())
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/", "/login", "/admin/login", "/registro", "/verificar-dpi", "/procesar-registro", "/procesar-login", "/css/**", "/js/**").permitAll()
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/usuarios/registrar").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                                .requestMatchers("/admin/**").hasAnyRole("ADMINISTRADOR")
                                .requestMatchers("/paciente/**", "/citas/**", "/pago/**").hasAnyRole("PACIENTE", "ADMINISTRADOR")
                                .requestMatchers("/dashboard").authenticated()
                                .requestMatchers("/recepcion/**").hasAnyRole("RECEPCIONISTA", "ADMINISTRADOR")
                                .requestMatchers("/caja/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                                .requestMatchers("/enfermeria/**").hasAnyRole("ENFERMERO", "ADMINISTRADOR")
                                .requestMatchers("/medico/**").hasAnyRole("MEDICO", "ADMINISTRADOR")
                                .requestMatchers("/laboratorio/**").hasAnyRole("LABORATORISTA", "ADMINISTRADOR")
                                .requestMatchers("/farmacia/**").hasAnyRole("FARMACEUTICO", "ADMINISTRADOR")
                                .requestMatchers("/inventario/**").hasAnyRole("FARMACEUTICO", "ADMINISTRADOR")
                                .anyRequest().authenticated()
                        )

                        .formLogin(form -> form
                                .loginPage("/login")
                                .permitAll()
                        )
                        .logout(logout -> logout
                                .logoutUrl("/logout")
                                // FA09 - CU-00 INICIO
                                // CU-00 FA09: el paciente vuelve al login del portal; el personal, al del panel administrativo
                                .logoutSuccessHandler((request, response, authentication) -> {
                                    boolean paciente = authentication != null && authentication.getAuthorities().stream()
                                            .anyMatch(a -> "ROLE_PACIENTE".equals(a.getAuthority()));
                                    response.sendRedirect(paciente ? "/login?logout=true" : "/admin/login?logout=true");
                                })
                                // FA09 - CU-00 FINAL
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .permitAll()
                        )

                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // permite sesiones
                                .sessionFixation().migrateSession()                       // previene fijacion de sesion
                        )
                        .securityContext(sc -> sc.securityContextRepository(securityContextRepository())) // Guarda el contexto en sesión HTTP
                        .authenticationProvider(authenticationProvider())
                        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
            }

            @Bean
            public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
            }

            @Bean
            public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
            }

            @Bean
            public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
            }
            @Bean
            public SecurityContextRepository securityContextRepository() {
                return new HttpSessionSecurityContextRepository();
            }

        }
