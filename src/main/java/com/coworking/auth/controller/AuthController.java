package com.coworking.auth.controller;
import com.coworking.auth.dto.*;
import com.coworking.auth.model.VerificationToken;
import com.coworking.auth.repository.VerificationTokenRepository;
import com.coworking.auth.service.AuthService;
import com.coworking.dto.common.ApiResponseDto;
import com.coworking.exception.NotFoundException;
import com.coworking.role.model.Role;
import com.coworking.security.JwtUtil;
import com.coworking.user.model.User;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.repository.UserRepository;

import com.coworking.email.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.coworking.exception.BadRequestException;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Authentication", description = "Endpoint para registro y login de usuarios")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private  final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
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
    public ResponseEntity<ApiResponseDto<Void>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                ApiResponseDto.success(authService.register(request))
        );
    }
    //VERIFY EMAIL
    @GetMapping("/verify")
    public void verify(@RequestParam String token,
                       HttpServletResponse response) throws IOException {
        authService.verifyAccount(token, response);
    }

    //REENVIAR CORREO DE VERIFICACION
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponseDto<String>> resendVerification(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        authService.resendVerification(request.get("email"))
                )
        );
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
    //LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        "Login exitoso",
                        authService.login(request)
                )
        );
    }

    // USER
    @GetMapping("/user")
    public ResponseEntity<ApiResponseDto<AuthResponse>> getUser(@RequestHeader("Authorization") String header){

        String token = header.replace("Bearer ", "");

        return ResponseEntity.ok(
                ApiResponseDto.success(
                        "Usuario obtenido",
                        authService.getUser(token)
                )
        );
    }

    // ADMIN
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> registerAdmin(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        authService.registerAdmin(request)
                )
        );
    }

    // FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDto<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        authService.forgotPassword(request)
                )
        );
    }

    // RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        authService.resetPassword(request)
                )
        );
    }

    //GOOGLE AUTH
    @PostMapping("/google")
    public ResponseEntity<ApiResponseDto<AuthResponse>> googleAuth(@RequestBody GoogleAuthRequest request){
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        "Login con Google exitoso",
                        authService.googleAuth(request)
                )
        );
    }

}
