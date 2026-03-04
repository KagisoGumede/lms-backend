package com.example.lms.service;

import com.example.lms.dto.DepartmentResponse;
import com.example.lms.dto.LeaveTypeResponse;
import com.example.lms.dto.PositionResponse;
import com.example.lms.model.Department;
import com.example.lms.model.LeaveType;
import com.example.lms.model.Position;
import com.example.lms.repository.DepartmentRepository;
import com.example.lms.repository.LeaveTypeRepository;
import com.example.lms.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepo;
    private final PositionRepository positionRepo;
    private final LeaveTypeRepository leaveTypeRepo;

    public DepartmentService(DepartmentRepository departmentRepo,
                             PositionRepository positionRepo,
                             LeaveTypeRepository leaveTypeRepo) {
        this.departmentRepo = departmentRepo;
        this.positionRepo = positionRepo;
        this.leaveTypeRepo = leaveTypeRepo;
    }

    // ─── DEPARTMENTS ─────────────────────────────────────────

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepo.findAll()
                .stream().map(DepartmentResponse::from).collect(Collectors.toList());
    }

    public DepartmentResponse addDepartment(String name) {
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department name is required");
        if (departmentRepo.existsByName(name.trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");

        Department dept = new Department(name.trim());
        departmentRepo.save(dept);
        return DepartmentResponse.from(dept);
    }

    public void deleteDepartment(Long id) {
        Department dept = departmentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        departmentRepo.delete(dept);
    }

    // ─── POSITIONS ───────────────────────────────────────────

    public List<PositionResponse> getPositionsByDepartment(Long departmentId) {
        return positionRepo.findByDepartmentId(departmentId)
                .stream().map(PositionResponse::from).collect(Collectors.toList());
    }

    public List<PositionResponse> getAllPositions() {
        return positionRepo.findAll()
                .stream().map(PositionResponse::from).collect(Collectors.toList());
    }

    public PositionResponse addPosition(String name, Long departmentId) {
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position name is required");

        Department dept = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));

        if (positionRepo.existsByNameAndDepartmentId(name.trim(), departmentId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Position already exists in this department");

        Position position = new Position(name.trim(), dept);
        positionRepo.save(position);
        return PositionResponse.from(position);
    }

    public void deletePosition(Long id) {
        Position position = positionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));
        positionRepo.delete(position);
    }

    // ─── LEAVE TYPES ─────────────────────────────────────────

    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepo.findAll()
                .stream().map(LeaveTypeResponse::from).collect(Collectors.toList());
    }

    public LeaveTypeResponse addLeaveType(String name) {
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave type name is required");
        if (leaveTypeRepo.existsByName(name.trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave type already exists");

        LeaveType leaveType = new LeaveType(name.trim());
        leaveTypeRepo.save(leaveType);
        return LeaveTypeResponse.from(leaveType);
    }

    public void deleteLeaveType(Long id) {
        LeaveType leaveType = leaveTypeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave type not found"));
        leaveTypeRepo.delete(leaveType);
    }
}