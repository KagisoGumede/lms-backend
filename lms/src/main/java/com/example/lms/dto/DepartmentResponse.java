package com.example.lms.dto;

import com.example.lms.model.Department;
import java.util.List;
import java.util.stream.Collectors;

public class DepartmentResponse {
    public Long id;
    public String name;
    public List<PositionResponse> positions;

    public static DepartmentResponse from(Department d) {
        DepartmentResponse res = new DepartmentResponse();
        res.id = d.getId();
        res.name = d.getName();
        res.positions = d.getPositions() != null
            ? d.getPositions().stream().map(PositionResponse::from).collect(Collectors.toList())
            : List.of();
        return res;
    }
}