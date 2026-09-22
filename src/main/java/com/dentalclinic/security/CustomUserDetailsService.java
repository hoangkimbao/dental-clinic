package com.dentalclinic.security;

import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> username.equals(u.getPhone()))
                        .findFirst()
                        .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username)));

        return new CustomUserDetails(user);
    }
}
