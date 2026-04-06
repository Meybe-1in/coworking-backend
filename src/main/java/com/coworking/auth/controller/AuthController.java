package com.coworking.auth.controller;
import com.coworking.auth.dto.*;
import com.coworking.auth.repository.VerificationTokenRepository;
import com.coworking.auth.service.AuthService;
import com.coworking.dto.common.ApiResponseDto;
import com.coworking.security.JwtUtil;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

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
        String message = authService.register(request);
        return ResponseEntity.ok(
                ApiResponseDto.success(message, null)
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
        String message = authService.resendVerification(request.get("email"));

        return ResponseEntity.ok(
                ApiResponseDto.success(message, null)
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
        AuthResponse data = authService.login(request);

        return ResponseEntity.ok(
                ApiResponseDto.success("Login exitoso", data)
        );
    }

    // USER
    @GetMapping("/user")
    public ResponseEntity<ApiResponseDto<AuthResponse>> getUser(@RequestHeader("Authorization") String header){

        String token = header.replace("Bearer ", "");

        AuthResponse data = authService.getUser(token);

        return ResponseEntity.ok(
                ApiResponseDto.success("Usuario obtenido", data)
        );
    }

    // ADMIN
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> registerAdmin(@RequestBody RegisterRequest request) {
        String message = authService.registerAdmin(request);

        return ResponseEntity.ok(
                ApiResponseDto.success(message, null)
        );
    }

    // FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDto<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponseDto.success(message, null)
        );
    }

    // RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponseDto.success(message, null)
        );
    }

    //GOOGLE AUTH
    @PostMapping("/google")
    public ResponseEntity<ApiResponseDto<AuthResponse>> googleAuth(@RequestBody GoogleAuthRequest request){
        AuthResponse data = authService.googleAuth(request);

        return ResponseEntity.ok(
                ApiResponseDto.success("Login con Google exitoso", data)
        );
    }

}
