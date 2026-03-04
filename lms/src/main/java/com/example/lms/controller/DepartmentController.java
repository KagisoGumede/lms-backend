package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.dto.DepartmentResponse;
import com.example.lms.dto.LeaveTypeResponse;
import com.example.lms.dto.PositionResponse;
import com.example.lms.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // ─── DEPARTMENTS ─────────────────────────────────────────

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.ok("Departments fetched", departmentService.getAllDepartments()));
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<DepartmentResponse>> addDepartment(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Department added", departmentService.addDepartment(body.get("name"))));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted", null));
    }

    // ─── POSITIONS ───────────────────────────────────────────

    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<PositionResponse>>> getAllPositions() {
        return ResponseEntity.ok(ApiResponse.ok("Positions fetched", departmentService.getAllPositions()));
    }

    @GetMapping("/positions/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<PositionResponse>>> getPositionsByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.ok("Positions fetched", departmentService.getPositionsByDepartment(departmentId)));
    }

    @PostMapping("/positions")
    public ResponseEntity<ApiResponse<PositionResponse>> addPosition(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Position added", departmentService.addPosition(
                body.get("name"),
                Long.parseLong(body.get("departmentId"))
        )));
    }

    @DeleteMapping("/positions/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePosition(@PathVariable Long id) {
        departmentService.deletePosition(id);
        return ResponseEntity.ok(ApiResponse.ok("Position deleted", null));
    }

    // ─── LEAVE TYPES ─────────────────────────────────────────

    @GetMapping("/leave-types")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> getAllLeaveTypes() {
        return ResponseEntity.ok(ApiResponse.ok("Leave types fetched", departmentService.getAllLeaveTypes()));
    }

    @PostMapping("/leave-types")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> addLeaveType(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Leave type added", departmentService.addLeaveType(body.get("name"))));
    }

    @DeleteMapping("/leave-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeaveType(@PathVariable Long id) {
        departmentService.deleteLeaveType(id);
        return ResponseEntity.ok(ApiResponse.ok("Leave type deleted", null));
    }
}
