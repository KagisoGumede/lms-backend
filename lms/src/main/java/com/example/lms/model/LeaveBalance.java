package com.example.lms.model;



import jakarta.persistence.*;

@Entity
@Table(name = "leave_balances")
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String leaveType;

    @Column(nullable = false)
    private int allocatedDays;

    public Long getId() { return id; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public int getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(int allocatedDays) { this.allocatedDays = allocatedDays; }
}