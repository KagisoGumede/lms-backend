package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.model.PublicHoliday;
import com.example.lms.repository.PublicHolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin
public class SettingsController {

    private final PublicHolidayRepository holidayRepo;

    public SettingsController(PublicHolidayRepository holidayRepo) {
        this.holidayRepo = holidayRepo;
    }

    // ─── Public Holidays ─────────────────────────────────────────

    @GetMapping("/public-holidays")
    public ResponseEntity<ApiResponse<List<PublicHoliday>>> getPublicHolidays() {
        return ResponseEntity.ok(ApiResponse.ok("Public holidays fetched",
                holidayRepo.findAllByOrderByDateAsc()));
    }

    @PostMapping("/public-holidays")
    public ResponseEntity<ApiResponse<PublicHoliday>> addPublicHoliday(@RequestBody Map<String, String> body) {
        String name    = body.get("name");
        String dateStr = body.get("date");
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holiday name is required");
        if (dateStr == null || dateStr.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holiday date is required");

        LocalDate date = LocalDate.parse(dateStr);
        if (holidayRepo.existsByDate(date))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A public holiday already exists on this date");

        PublicHoliday holiday = new PublicHoliday();
        holiday.setName(name.trim());
        holiday.setDate(date);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Public holiday added", holidayRepo.save(holiday)));
    }

    @DeleteMapping("/public-holidays/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePublicHoliday(@PathVariable Long id) {
        if (!holidayRepo.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Public holiday not found");
        holidayRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Public holiday deleted", null));
    }
}