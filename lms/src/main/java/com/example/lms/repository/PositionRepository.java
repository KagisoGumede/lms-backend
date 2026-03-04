package com.example.lms.repository;

import com.example.lms.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByDepartmentId(Long departmentId);
    boolean existsByNameAndDepartmentId(String name, Long departmentId);
}