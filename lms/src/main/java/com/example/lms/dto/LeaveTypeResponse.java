package com.example.lms.dto;

import com.example.lms.model.LeaveType;

public class LeaveTypeResponse {
    public Long id;
    public String name;

    public static LeaveTypeResponse from(LeaveType lt) {
        LeaveTypeResponse res = new LeaveTypeResponse();
        res.id = lt.getId();
        res.name = lt.getName();
        return res;
    }
}
