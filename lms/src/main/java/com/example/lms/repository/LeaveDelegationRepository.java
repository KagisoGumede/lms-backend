package com.example.lms.repository;



import com.example.lms.model.LeaveDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeaveDelegationRepository extends JpaRepository<LeaveDelegation, Long> {

    // Get active delegation created by this manager
    Optional<LeaveDelegation> findByDelegatorIdAndActiveTrue(Long delegatorId);

    // Get all active delegations assigned TO this manager
    List<LeaveDelegation> findByDelegateIdAndActiveTrue(Long delegateId);
}