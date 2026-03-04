package com.example.lms.service;



import com.example.lms.model.LeaveDelegation;
import com.example.lms.model.User;
import com.example.lms.repository.LeaveDelegationRepository;
import com.example.lms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DelegationService {

    private final LeaveDelegationRepository delegationRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public DelegationService(LeaveDelegationRepository delegationRepo,
                              UserRepository userRepo,
                              AuditLogService auditLogService,
                              NotificationService notificationService) {
        this.delegationRepo = delegationRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    // Manager sets a delegation
    public LeaveDelegation delegate(Long delegatorId, Long delegateId, LocalDate expiryDate) {
        if (delegatorId.equals(delegateId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot delegate to yourself");

        User delegator = userRepo.findById(delegatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));
        User delegate = userRepo.findById(delegateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delegate manager not found"));

        if (expiryDate.isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date must be in the future");

        // Deactivate any existing delegation
        delegationRepo.findByDelegatorIdAndActiveTrue(delegatorId)
                .ifPresent(d -> { d.setActive(false); delegationRepo.save(d); });

        LeaveDelegation delegation = new LeaveDelegation();
        delegation.setDelegator(delegator);
        delegation.setDelegate(delegate);
        delegation.setExpiryDate(expiryDate);
        delegation.setActive(true);
        delegationRepo.save(delegation);

        // Audit log
        auditLogService.log(
            "DELEGATION_SET",
            delegator.getFirstName() + " " + delegator.getSurname(),
            "MANAGER",
            delegate.getFirstName() + " " + delegate.getSurname(),
            "Delegated approvals until " + expiryDate
        );

        // Notify delegate
        notificationService.send(
            delegateId,
            "Approval Delegation",
            delegator.getFirstName() + " " + delegator.getSurname()
                + " has delegated their leave approvals to you until " + expiryDate,
            "DELEGATION_SET"
        );

        return delegation;
    }

    // Manager removes delegation
    public void removeDelegation(Long delegatorId) {
        delegationRepo.findByDelegatorIdAndActiveTrue(delegatorId).ifPresent(d -> {
            User delegator = d.getDelegator();
            User delegate = d.getDelegate();

            d.setActive(false);
            delegationRepo.save(d);

            auditLogService.log(
                "DELEGATION_REMOVED",
                delegator.getFirstName() + " " + delegator.getSurname(),
                "MANAGER",
                delegate.getFirstName() + " " + delegate.getSurname(),
                "Delegation removed"
            );

            notificationService.send(
                delegate.getId(),
                "Delegation Removed",
                delegator.getFirstName() + " " + delegator.getSurname()
                    + " has removed your delegation for their team",
                "DELEGATION_REMOVED"
            );
        });
    }

    // Get current active delegation for a manager (as delegator)
    public Optional<LeaveDelegation> getActiveDelegation(Long delegatorId) {
        return delegationRepo.findByDelegatorIdAndActiveTrue(delegatorId)
                .filter(d -> !d.getExpiryDate().isBefore(LocalDate.now()));
    }

    // Get all teams delegated TO this manager
    public List<LeaveDelegation> getDelegatedToMe(Long delegateId) {
        return delegationRepo.findByDelegateIdAndActiveTrue(delegateId)
                .stream()
                .filter(d -> !d.getExpiryDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
    }
}