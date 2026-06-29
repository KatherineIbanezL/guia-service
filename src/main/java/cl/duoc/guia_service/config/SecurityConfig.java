package cl.duoc.guia_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF 
            .csrf(csrf -> csrf.disable())
            
            // Configurar reglas de autorización de rutas
            .authorizeRequests(auth -> auth
                // 1. Permitir acceso público solo al endpoint de salud del sistema
                .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                
                // 2. Un rol permite SOLO usar el endpoint de Descargar guías
                // Ambos roles (descargar y operador) pueden entrar aquí
                .antMatchers(HttpMethod.GET, "/api/documentos/descargar/**").hasAnyRole("descargar", "operador")
                
                // 3. El rol "operador" permite el uso de TODO el resto de los endpoints
                .antMatchers("/api/documentos/**").hasRole("operador")
                
                // Cualquier otra ruta requerirá obligatoriamente estar autenticado
                .anyRequest().authenticated()
            )
            
            // Configurar el servidor de recursos OAuth2 usando el convertidor personalizado
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    // El Bean que asocia el método extractor al flujo de autenticación de Spring Security
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromConsultaRole);
        return converter;
    }

    // Método extractor para procesar atributo "extension_consultaRole"
    private Collection<GrantedAuthority> extractAuthoritiesFromConsultaRole(Jwt jwt) {
        // Lee el claim personalizado que creaste en tu panel de Azure AD B2C
        Object claim = jwt.getClaim("extension_consultaRole");

        // Si el claim viene como un texto simple (String)
        if (claim instanceof String && !((String) claim).isBlank()) {
            String role = (String) claim;
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.trim().toLowerCase()));
        }

        // Si el claim viene como una lista/colección de roles
        if (claim instanceof Collection) {
            Collection<?> roles = (Collection<?>) claim;
            return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toLowerCase()))
                .collect(Collectors.toList());
        }

        // Si no trae ningún rol, se retorna una lista vacía (sin permisos de aplicación)
        return List.of();
    }
}