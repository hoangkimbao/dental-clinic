package com.dentalclinic.service;

import com.dentalclinic.dto.*;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.Role;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.security.CustomUserDetails;
import com.dentalclinic.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final NotificationService notificationService;
    private final com.dentalclinic.security.LoginAttemptService loginAttemptService;

    // 100% Constructor Injection
    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       NotificationService notificationService,
                       com.dentalclinic.security.LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.notificationService = notificationService;
        this.loginAttemptService = loginAttemptService;
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        if (loginAttemptService.isBlocked(username)) {
            throw new BadRequestException("Tài khoản tạm thời bị khóa do đăng nhập sai quá 5 lần. Vui lòng thử lại sau 15 phút!");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword().trim())
            );

            loginAttemptService.loginSucceeded(username);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String jwt = tokenProvider.generateToken(authentication, user.getId(), user.getFullName());

            return new AuthResponse(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.getPhone(),
                    user.getEmail()
            );
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            loginAttemptService.loginFailed(username);
            throw ex;
        }
    }

    @Transactional
    public AuthResponse registerPatient(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.");
        }

        User newUser = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword().trim()),
                request.getFullName().trim(),
                request.getPhone().trim(),
                request.getEmail() != null ? request.getEmail().trim() : "",
                Role.ROLE_PATIENT
        );

        User saved = userRepository.save(newUser);

        notificationService.sendNotification(
                "ROLE_RECEPTIONIST",
                "👤 BỆNH NHÂN MỚI TẠO TÀI KHOẢN",
                "Khách hàng: " + saved.getFullName() + " (" + saved.getPhone() + ") vừa đăng ký tài khoản.",
                "NEW_PATIENT"
        );

        return login(new LoginRequest() {{
            setUsername(request.getUsername());
            setPassword(request.getPassword());
        }});
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        String iden = request.getIdentifier().trim();
        User user = userRepository.findByUsername(iden)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> iden.equals(u.getPhone()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với thông tin: " + iden)));

        // Sinh mã OTP / Token reset mật khẩu mô phỏng gửi qua SMS/Email
        String resetToken = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        notificationService.sendNotification(
                user.getUsername(),
                "🔐 MÃ XÁC THỰC KHÔI PHỤC MẬT KHẨU",
                "Mã xác thực OTP của bạn là: " + resetToken + " (Có hiệu lực trong 5 phút).",
                "SECURITY_ALERT"
        );

        return "Mã khôi phục OTP đã được gửi đến số điện thoại / email của bạn!";
    }
}
