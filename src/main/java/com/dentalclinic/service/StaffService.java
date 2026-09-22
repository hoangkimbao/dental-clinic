package com.dentalclinic.service;

import com.dentalclinic.dto.StaffCreateRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.StaffShiftRepository;
import com.dentalclinic.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StaffService {

    private final UserRepository userRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final PasswordEncoder passwordEncoder;

    // 100% Constructor Injection
    public StaffService(UserRepository userRepository,
                        StaffShiftRepository staffShiftRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createStaff(StaffCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
            throw new BadRequestException("Tên đăng nhập nhân viên này đã tồn tại!");
        }

        User staff = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword().trim()),
                request.getFullName().trim(),
                request.getPhone() != null ? request.getPhone().trim() : "",
                "",
                request.getRole()
        );

        User saved = userRepository.save(staff);

        String shiftRoleTitle = switch (request.getRole()) {
            case ROLE_DENTIST -> "Bác Sĩ Trực Khám";
            case ROLE_RECEPTIONIST -> "Lễ Tân Điều Phối";
            case ROLE_ASSISTANT -> "Phụ Tá Dụng Cụ";
            case ROLE_CLEANER -> "Vô Trùng Phòng Khám";
            default -> "Nhân Viên";
        };

        StaffShift initialShift = new StaffShift(saved, LocalDate.now(), ShiftType.CA_SANG_8H_12H, shiftRoleTitle, "Ca trực phân công ban đầu");
        staffShiftRepository.save(initialShift);

        return saved;
    }

    public List<User> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ROLE_PATIENT)
                .toList();
    }

    public List<User> getDentists() {
        return userRepository.findByRole(Role.ROLE_DENTIST);
    }
}
