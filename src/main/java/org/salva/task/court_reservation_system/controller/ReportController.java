package org.salva.task.court_reservation_system.controller;

import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.ReportFilterDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO;
import org.salva.task.court_reservation_system.enums.SportType;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.service.ReportExportService;
import org.salva.task.court_reservation_system.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final ReportExportService exportService;

    @GetMapping("/operational")
    public OperationalReportResponseDTO operational(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) SportType sportType,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return reportService.build(new ReportFilterDTO(startDate, endDate, venueId, courtId, sportType), currentUser);
    }

    @GetMapping("/operational/export")
    public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) SportType sportType,
            @RequestParam(defaultValue = "csv") String format,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OperationalReportResponseDTO report = reportService.build(new ReportFilterDTO(startDate, endDate, venueId, courtId, sportType), currentUser);
        String baseName = "reporte-operativo-" + report.getStartDate() + "_" + report.getEndDate();
        return switch (format.toLowerCase()) {
            case "csv" -> file(exportService.csv(report), "text/csv;charset=UTF-8", baseName + ".csv");
            case "xlsx" -> file(exportService.xlsx(report), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", baseName + ".xlsx");
            case "pdf" -> file(exportService.pdf(report), "application/pdf", baseName + ".pdf");
            default -> throw new ValidationException("Formato no soportado. Usa csv, xlsx o pdf");
        };
    }

    private ResponseEntity<byte[]> file(byte[] content, String contentType, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
