package org.salva.task.court_reservation_system.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.CourtStatDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.DayStatDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.PeakHourDTO;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO.SportStatDTO;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExportServiceTest {

    private final ReportExportService service = new ReportExportService();

    @Test
    void xlsxIsAReadableWorkbookWithNumericMoney() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(service.xlsx(report())))) {
            assertEquals(5, workbook.getNumberOfSheets());
            Sheet summary = workbook.getSheet("Resumen");
            assertEquals("Reservas totales", summary.getRow(3).getCell(0).getStringCellValue());
            assertEquals(3, summary.getRow(3).getCell(1).getNumericCellValue());
            assertEquals(150.0, summary.getRow(7).getCell(1).getNumericCellValue());
            Sheet courts = workbook.getSheet("Por cancha");
            assertEquals("Pádel, 1", courts.getRow(1).getCell(0).getStringCellValue());
            assertEquals(150.0, courts.getRow(1).getCell(4).getNumericCellValue());
        }
    }

    @Test
    void pdfHasPdfSignature() {
        byte[] pdf = service.pdf(report());
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void csvQuotesValuesWithCommas() {
        String csv = new String(service.csv(report()), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("﻿Indicador,Valor"));
        assertTrue(csv.contains("\"Pádel, 1\""));
        assertTrue(csv.contains("Hora pico,Reservas"));
    }

    private OperationalReportResponseDTO report() {
        return OperationalReportResponseDTO.builder().startDate("2026-03-01").endDate("2026-03-03").scope("Sede: todas · Cancha: todas · Deporte: todos")
                .totalBookings(3).completedBookings(1).cancelledBookings(1).noShows(1)
                .revenue(new BigDecimal("150.00")).cancellationRate(new BigDecimal("33.33"))
                .peakHours(List.of(PeakHourDTO.builder().hour("18:00").bookings(2).build()))
                .daily(List.of(DayStatDTO.builder().date("2026-03-01").bookings(2).revenue(new BigDecimal("100")).build()))
                .byCourt(List.of(CourtStatDTO.builder().courtId(1L).courtName("Pádel, 1").sportType("PADEL").venueName("Central").bookings(3).revenue(new BigDecimal("150")).build()))
                .bySport(List.of(SportStatDTO.builder().sportType("PADEL").bookings(3).revenue(new BigDecimal("150")).build()))
                .build();
    }
}
