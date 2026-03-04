package com.example.lms.service;



import com.example.lms.model.LeaveBalance;
import com.example.lms.model.LeaveStatus;
import com.example.lms.repository.LeaveBalanceRepository;
import com.example.lms.repository.LeaveRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepo;
    private final LeaveRequestRepository leaveRepo;

    public LeaveBalanceService(LeaveBalanceRepository balanceRepo,
                                LeaveRequestRepository leaveRepo) {
        this.balanceRepo = balanceRepo;
        this.leaveRepo = leaveRepo;
    }

    // Admin: set or update balance for a leave type
    public LeaveBalance setBalance(String leaveType, int allocatedDays) {
        if (allocatedDays < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocated days cannot be negative");

        LeaveBalance balance = balanceRepo.findByLeaveType(leaveType)
                .orElse(new LeaveBalance());
        balance.setLeaveType(leaveType);
        balance.setAllocatedDays(allocatedDays);
        return balanceRepo.save(balance);
    }

    // Get all configured balances
    public List<LeaveBalance> getAllBalances() {
        return balanceRepo.findAll();
    }

    // Get balance summary for a specific employee
    public List<Map<String, Object>> getEmployeeBalances(Long employeeId) {
        List<LeaveBalance> balances = balanceRepo.findAll();
        var approvedLeaves = leaveRepo.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .collect(Collectors.toList());

        return balances.stream().map(b -> {
            int used = approvedLeaves.stream()
                    .filter(l -> l.getLeaveType().equals(b.getLeaveType()))
                    .mapToInt(l -> l.getDuration())
                    .sum();
            int remaining = Math.max(0, b.getAllocatedDays() - used);
            int pct = b.getAllocatedDays() > 0
                    ? (int) Math.round(((double) used / b.getAllocatedDays()) * 100)
                    : 0;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("leaveType",     b.getLeaveType());
            entry.put("allocated",     b.getAllocatedDays());
            entry.put("used",          used);
            entry.put("remaining",     remaining);
            entry.put("usedPercent",   pct);
            entry.put("isLow",         remaining <= 3 && b.getAllocatedDays() > 0);
            entry.put("isExhausted",   remaining == 0 && b.getAllocatedDays() > 0);
            return entry;
        }).collect(Collectors.toList());
    }
}