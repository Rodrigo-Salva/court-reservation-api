package org.salva.task.court_reservation_system.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.salva.task.court_reservation_system.dto.response.OperationalReportResponseDTO;
import org.salva.task.court_reservation_system.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/** Genera el reporte operativo en CSV, XLSX (Apache POI) y PDF (OpenPDF). */
@Service
public class ReportExportService {

    public byte[] csv(OperationalReportResponseDTO report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Indicador,Valor\n")
                .append("Periodo,").append(report.getStartDate()).append(" al ").append(report.getEndDate()).append('\n')
                .append("Alcance,").append(cell(report.getScope())).append('\n')
                .append("Reservas totales,").append(report.getTotalBookings()).append('\n')
                .append("Reservas completadas,").append(report.getCompletedBookings()).append('\n')
                .append("Cancelaciones,").append(report.getCancelledBookings()).append('\n')
                .append("No show,").append(report.getNoShows()).append('\n')
                .append("Ingresos,").append(report.getRevenue()).append('\n')
                .append("Tasa de cancelacion,").append(report.getCancellationRate()).append("%\n\n");
        csv.append("Cancha,Deporte,Sede,Reservas,Ingresos\n");
        report.getByCourt().forEach(c -> csv.append(cell(c.getCourtName())).append(',').append(c.getSportType()).append(',')
                .append(cell(c.getVenueName())).append(',').append(c.getBookings()).append(',').append(c.getRevenue()).append('\n'));
        csv.append("\nDeporte,Reservas,Ingresos\n");
        report.getBySport().forEach(s -> csv.append(s.getSportType()).append(',').append(s.getBookings()).append(',').append(s.getRevenue()).append('\n'));
        csv.append("\nFecha,Reservas,Ingresos\n");
        report.getDaily().forEach(d -> csv.append(d.getDate()).append(',').append(d.getBookings()).append(',').append(d.getRevenue()).append('\n'));
        csv.append("\nHora pico,Reservas\n");
        report.getPeakHours().forEach(h -> csv.append(h.getHour()).append(',').append(h.getBookings()).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] xlsx(OperationalReportResponseDTO report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("\"S/\" #,##0.00"));
            CellStyle percent = workbook.createCellStyle();
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.00\"%\""));

            Sheet summary = workbook.createSheet("Resumen");
            row(summary, 0, header, "Indicador", "Valor");
            row(summary, 1, null, "Periodo", report.getStartDate() + " al " + report.getEndDate());
            row(summary, 2, null, "Alcance", report.getScope());
            row(summary, 3, null, "Reservas totales", report.getTotalBookings());
            row(summary, 4, null, "Reservas completadas", report.getCompletedBookings());
            row(summary, 5, null, "Cancelaciones", report.getCancelledBookings());
            row(summary, 6, null, "No show", report.getNoShows());
            row(summary, 7, null, "Ingresos", report.getRevenue()).getCell(1).setCellStyle(money);
            row(summary, 8, null, "Tasa de cancelación", report.getCancellationRate()).getCell(1).setCellStyle(percent);
            widths(summary, 28, 60);

            Sheet courts = workbook.createSheet("Por cancha");
            row(courts, 0, header, "Cancha", "Deporte", "Sede", "Reservas", "Ingresos");
            int i = 1;
            for (var c : report.getByCourt()) row(courts, i++, null, c.getCourtName(), c.getSportType(), c.getVenueName() == null ? "" : c.getVenueName(), c.getBookings(), c.getRevenue()).getCell(4).setCellStyle(money);
            widths(courts, 30, 16, 24, 12, 16);

            Sheet sports = workbook.createSheet("Por deporte");
            row(sports, 0, header, "Deporte", "Reservas", "Ingresos");
            i = 1;
            for (var s : report.getBySport()) row(sports, i++, null, s.getSportType(), s.getBookings(), s.getRevenue()).getCell(2).setCellStyle(money);
            widths(sports, 18, 12, 16);

            Sheet daily = workbook.createSheet("Diario");
            row(daily, 0, header, "Fecha", "Reservas", "Ingresos");
            i = 1;
            for (var d : report.getDaily()) row(daily, i++, null, d.getDate(), d.getBookings(), d.getRevenue()).getCell(2).setCellStyle(money);
            widths(daily, 14, 12, 16);

            Sheet peaks = workbook.createSheet("Horas pico");
            row(peaks, 0, header, "Hora", "Reservas");
            i = 1;
            for (var h : report.getPeakHours()) row(peaks, i++, null, h.getHour(), h.getBookings());
            widths(peaks, 12, 12);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar el archivo XLSX");
        }
    }

    public byte[] pdf(OperationalReportResponseDTO report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font section = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

            document.add(new Paragraph("Reporte operativo", title));
            document.add(new Paragraph("Periodo: " + report.getStartDate() + " al " + report.getEndDate(), small));
            document.add(new Paragraph(report.getScope(), small));
            document.add(new Paragraph(" "));

            PdfPTable kpis = table(2, 60);
            kv(kpis, "Reservas totales", String.valueOf(report.getTotalBookings()));
            kv(kpis, "Reservas completadas", String.valueOf(report.getCompletedBookings()));
            kv(kpis, "Cancelaciones", report.getCancelledBookings() + " (" + report.getCancellationRate() + "%)");
            kv(kpis, "No show", String.valueOf(report.getNoShows()));
            kv(kpis, "Ingresos", money(report.getRevenue()));
            document.add(kpis);

            document.add(spaced("Ingresos por cancha", section));
            PdfPTable courts = table(4, 100);
            head(courts, "Cancha", "Deporte", "Reservas", "Ingresos");
            report.getByCourt().forEach(c -> body(courts, c.getCourtName(), c.getSportType(), String.valueOf(c.getBookings()), money(c.getRevenue())));
            document.add(courts);

            document.add(spaced("Por deporte", section));
            PdfPTable sports = table(3, 70);
            head(sports, "Deporte", "Reservas", "Ingresos");
            report.getBySport().forEach(s -> body(sports, s.getSportType(), String.valueOf(s.getBookings()), money(s.getRevenue())));
            document.add(sports);

            document.add(spaced("Horas con mayor demanda", section));
            PdfPTable peaks = table(2, 40);
            head(peaks, "Hora", "Reservas");
            report.getPeakHours().forEach(h -> body(peaks, h.getHour(), String.valueOf(h.getBookings())));
            document.add(peaks);

            List<OperationalReportResponseDTO.DayStatDTO> activeDays = report.getDaily().stream().filter(d -> d.getBookings() > 0).collect(Collectors.toList());
            document.add(spaced("Detalle diario (días con reservas)", section));
            PdfPTable daily = table(3, 70);
            head(daily, "Fecha", "Reservas", "Ingresos");
            activeDays.forEach(d -> body(daily, d.getDate(), String.valueOf(d.getBookings()), money(d.getRevenue())));
            document.add(daily);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new BusinessException("No se pudo generar el archivo PDF");
        }
    }

    private Paragraph spaced(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private PdfPTable table(int columns, float widthPercent) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(widthPercent);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        return table;
    }

    private void kv(PdfPTable table, String key, String value) {
        body(table, key, value);
    }

    private void head(PdfPTable table, String... labels) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, font));
            cell.setBackgroundColor(new Color(34, 139, 84));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private void body(PdfPTable table, String... values) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private String money(BigDecimal amount) {
        return "S/ " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private Row row(Sheet sheet, int index, CellStyle style, Object... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object value = values[i];
            if (value instanceof BigDecimal decimal) cell.setCellValue(decimal.doubleValue());
            else if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(value == null ? "" : value.toString());
            if (style != null) cell.setCellStyle(style);
        }
        return row;
    }

    private void widths(Sheet sheet, int... characters) {
        for (int i = 0; i < characters.length; i++) sheet.setColumnWidth(i, characters[i] * 256);
    }

    private String cell(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.matches(".*[,\"\n].*") ? "\"" + escaped + "\"" : escaped;
    }
}
