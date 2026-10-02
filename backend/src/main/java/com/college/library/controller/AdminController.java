package com.college.library.controller;

import com.college.library.dto.DashboardDtos.*;
import com.college.library.dto.MiscDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.service.AuditService;
import com.college.library.service.DashboardService;
import com.college.library.service.ReportService;
import com.college.library.service.SettingsService;
import com.college.library.util.CsvWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

/** Dashboards, reports, settings and audit logs. */
@RestController
@RequestMapping("/api")
@Tag(name = "Dashboards, reports, settings & audit")
public class AdminController {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    public AdminController(DashboardService dashboardService, ReportService reportService,
                           SettingsService settingsService, AuditService auditService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
        this.settingsService = settingsService;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- dashboards
    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard adminDashboard() {
        return dashboardService.admin();
    }

    @GetMapping("/dashboard/librarian")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public LibrarianDashboard librarianDashboard() {
        return dashboardService.librarian();
    }

    @GetMapping("/dashboard/member")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberDashboard memberDashboard() {
        return dashboardService.member();
    }

    // ---------------------------------------------------------------- reports
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "List available report types")
    public Map<String, String> reportTypes() {
        return ReportService.REPORT_TYPES;
    }

    @GetMapping("/reports/{type}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Run a report as JSON, or as a CSV download with format=csv",
            description = "Types: borrowed, overdue, returned, fine-collection, outstanding-fines, most-borrowed, "
                    + "active-members, books-by-category, inventory, reservations")
    public ResponseEntity<?> report(@PathVariable String type,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(defaultValue = "json") String format) {
        ReportTable table = reportService.generate(type, from, to);
        if (!"csv".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(table);
        }
        // UTF-8 BOM so Excel shows the ₹ symbol correctly
        String csv = "\uFEFF" + CsvWriter.write(table.columns(), table.rows());
        String filename = type + "-report-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- settings
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public SettingsDto settings() {
        return settingsService.get();
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public SettingsDto updateSettings(@Valid @RequestBody SettingsDto dto) {
        return settingsService.update(dto);
    }

    // ---------------------------------------------------------------- audit logs
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditLogResponse> auditLogs(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String entityType,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return auditService.search(q, entityType, from, to, Paging.of(page, size, "createdAt", "desc"));
    }
}
