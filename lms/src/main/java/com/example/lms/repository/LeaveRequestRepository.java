package com.example.lms.repository;

import com.example.lms.model.DocumentStatus;
import com.example.lms.model.LeaveRequest;
import com.example.lms.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployeeManagerId(Long managerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LeaveRequest l WHERE l.employee.id = :employeeId")
    void deleteByEmployeeId(Long employeeId);

    @Modifying
    @Transactional
    @Query("UPDATE LeaveRequest l SET l.reviewedBy = null WHERE l.reviewedBy.id = :managerId")
    void clearReviewedBy(Long managerId);

    // ─── Find approved leaves where document is required,
    //     not yet uploaded, and deadline has passed ─────────────
    @Query("SELECT l FROM LeaveRequest l " +
           "WHERE l.status = com.example.lms.model.LeaveStatus.APPROVED " +
           "AND l.documentRequired = true " +
           "AND l.documentStatus = com.example.lms.model.DocumentStatus.PENDING " +
           "AND l.documentDeadline < :today")
    List<LeaveRequest> findOverdueDocuments(LocalDate today);
}