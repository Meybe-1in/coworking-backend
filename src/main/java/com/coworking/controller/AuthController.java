package com.coworking.controller;
import com.coworking.dto.AuthResponse;
import com.coworking.dto.LoginRequest;
import com.coworking.dto.RegisterRequest;
import com.coworking.security.JwtUtil;
import com.coworking.model.Role;
import com.coworking.model.User;
import com.coworking.repository.RoleRepository;
import com.coworking.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Authentication", description = "Endpoint para registro y login de usuarios")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private  final JwtUtil jwtUtil;

    //REGISTER
    @Operation(
            summary = "Registrar usuarios",
            description = "Crea nuevo usuario con rol ROLE_USER",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Usuario registrado"),
                    @ApiResponse(responseCode = "400", description = "Correo ya existe", content = @Content)
            }
    )
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request){

        if (Boolean.FALSE.equals(request.termsAccepted())){
            return ResponseEntity
                    .badRequest()
                    .body("Debe aceptar términos y condiciones");
        }

        if (userRepository.findByEmail(request.email()).isPresent()){
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "field", "email",
                            "message", "Correo ya está registrado"
                    ));
        }

        // Obtener rol
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role ROLE_USER no encontrado"));

        // Crear usuario
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(role));

        // Guardar
        userRepository.save(user);

        //UserDetails contruir
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(Role::getName).toArray(String[]::new))
                .build();

        // Generar token
        String token = jwtUtil.generateToken(userDetails, user.getUsername());
        // Obtener rol (primero)
        String userRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        // Devolver JSON con token
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), userRole));

    }


    //LOGIN
    @Operation(
            summary = "Login de usuario",
            description = "Autentica un usuario y devuelve un JWT",
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWT generado" ,
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciales invalidas", content = @Content)
            }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // crea usuario con su rol de usuario
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // pasamos objeto UserDetails
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Generar el token
            String token = jwtUtil.generateToken(userDetails, user.getUsername());

            //role
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority) //.map(Role::getName)
                    .orElse("ROLE_USER");

            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), role));

        } catch (
                BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Credenciales incorrectas"));
        } catch (Exception e) {
            // Esto imprimirá el error real en tu consola si vuelve a fallar
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error en el servidor: " + e.getMessage()));
        }
    }
    //user
    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestHeader("authorization") String header){
        String token =header.replace("Bearer ", "");
        String email = jwtUtil.extractUsername(token);
        String username = jwtUtil.extractUsernameUi(token);

        User user = userRepository.findByEmail(email).orElseThrow();

        String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        return ResponseEntity.ok(new AuthResponse(token, username, role));
    }

    //admin
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Registrar administradores",
            description = "Crea un nuevo usuario con rol ROLE_ADMIN (solo accesible para usuarios con rol ADMIN)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Administrador registrado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Correo ya existe", content = @Content),
                    @ApiResponse(responseCode = "403", description = "No autorizado (solo ADMIN puede crear otro ADMIN)", content = @Content)
            }
    )
    public ResponseEntity<Map<String, String>> registerAdmin(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Correo ya existe"));
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        // Buscar el rol ADMIN en la base de datos
        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

        user.setRoles(Set.of(roleAdmin));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Administrador registrado correctamente"));
    }
}
