package com.example.lms.dto;

import com.example.lms.model.Position;

public class PositionResponse {
    public Long id;
    public String name;
    public Long departmentId;
    public String departmentName;

    public static PositionResponse from(Position p) {
        PositionResponse res = new PositionResponse();
        res.id = p.getId();
        res.name = p.getName();
        res.departmentId = p.getDepartment().getId();
        res.departmentName = p.getDepartment().getName();
        return res;
    }
}