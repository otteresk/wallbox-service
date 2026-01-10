package net.aahso.homehausen.wallbox_service;

import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.Configuration; 
import org.springframework.security.config.Customizer; 
import org.springframework.security.config.annotation.web.builders.HttpSecurity; 
import org.springframework.security.core.userdetails.User; 
import org.springframework.security.core.userdetails.UserDetails; 
import org.springframework.security.core.userdetails.UserDetailsService; 
import org.springframework.security.provisioning.InMemoryUserDetailsManager; 
import org.springframework.security.web.SecurityFilterChain; 
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration 
public class SecurityConfig { 

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Define users 
    @Bean 
    public UserDetailsService userDetailsService(PasswordEncoder encoder) { 
        UserDetails user = User.builder() 
                 .username("u") 
                 .password(encoder.encode("p")) 
                 .roles("USER")
                 .build(); 

          return new InMemoryUserDetailsManager(user); 
    } 

    // Define security rules 
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { 
           http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth 
                    .requestMatchers("/status/**").permitAll() 
                    .requestMatchers("/error").permitAll() 
                    .requestMatchers("/request/**").hasAnyRole("USER") 
                    .anyRequest().authenticated() 
                ) 
                .httpBasic(Customizer.withDefaults()); 

            return http.build(); 
    } 
}
