package cl.duoc.guia_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF ya que usamos tokens JWT (Stateless)
            .csrf(csrf -> csrf.disable())
            
            // Configurar reglas de autorización de rutas
            .authorizeRequests(auth -> auth
                // Permitir acceso público solo al endpoint de salud del sistema
                .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Cualquier otra ruta (/api/documentos/**) requerirá obligatoriamente autenticación
                .anyRequest().authenticated()
            )
            
            // Configurar la aplicación como un servidor de recursos OAuth2 usando JWT
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
            
        return http.build();
    }
}