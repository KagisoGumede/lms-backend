package com.example.lms.repository;

import com.example.lms.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findAllByOrderByDateAsc();
    boolean existsByDate(LocalDate date);
}