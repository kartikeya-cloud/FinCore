package dev.fincore;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
 @Bean UserDetailsService users(JdbcTemplate db) {
  return email -> db.query("SELECT email,password_hash FROM app_users WHERE email=?",(rs,n) -> User.withUsername(rs.getString(1)).password(rs.getString(2)).roles("USER").build(),email.toLowerCase().trim())
   .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
 }
 @Bean SecurityFilterChain filter(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a -> a.requestMatchers("/api/csrf","/api/register","/api/login","/actuator/health","/","/index.html","/assets/**").permitAll().anyRequest().authenticated())
   .formLogin(f -> f.loginProcessingUrl("/api/login").usernameParameter("email")
    .successHandler((req,res,auth) -> {res.setContentType("application/json");res.getWriter().write("{\"ok\":true}");})
    .failureHandler((req,res,e) -> {res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Incorrect email or password.\"}");}))
   .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,a) -> res.setStatus(204)))
   .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> {res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Please sign in.\"}");})
    .accessDeniedHandler((req,res,ex) -> {res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Session token expired. Refresh and try again.\"}");}))
   .build();
 }
}
