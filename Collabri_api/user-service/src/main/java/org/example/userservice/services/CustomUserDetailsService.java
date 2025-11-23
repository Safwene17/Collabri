package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.Admin;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.AdminRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try loading as User first
        Optional<User> user = userRepository.findByEmail(username);
        if (user.isPresent()) {
            return user.get();
        }

        // Then try as Admin
        Optional<Admin> admin = adminRepository.findByEmail(username);
        if (admin.isPresent()) {
            return admin.get();
        }

        throw new UsernameNotFoundException("User or Admin not found with email: " + username);
    }
}