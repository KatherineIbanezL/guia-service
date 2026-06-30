package cl.duoc.guia_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeRequests(auth -> auth
                // Endpoint público de salud
                .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                
                // Un rol que permite SOLO usar el endpoint de Descargar guías
                .antMatchers(HttpMethod.GET, "/api/documentos/descargar/**").hasAuthority("ROLE_descargar")
                
                .antMatchers("/api/documentos/**").hasAuthority("ROLE_operador")
                
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    // Bean para decodificar y validar de forma local la firma con el set de llaves de Azure B2C
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = "https://katherineibanezb2c.b2clogin.com/katherineibanezb2c.onmicrosoft.com/discovery/v2.0/keys?p=b2c_1_app-guiaservice";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    // Claim
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        // nombre exacto del claim personalizado creado en Azure B2C
        grantedAuthoritiesConverter.setAuthoritiesClaimName("extension_consultaRole");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return authenticationConverter;
    }
}