package com.example.lms.repository;

import com.example.lms.model.Role;
import com.example.lms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAddress(String emailAddress);
    Optional<User> findByResetToken(String resetToken);
    List<User> findByRole(Role role);
    List<User> findByManagerId(Long managerId);
    List<User> findByDepartment(String department);
}