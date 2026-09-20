package org.salva.task.court_reservation_system.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class OperationalReportResponseDTO {
    private String startDate;
    private String endDate;
    private String scope;
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long noShows;
    private BigDecimal revenue;
    private BigDecimal cancellationRate;
    private List<PeakHourDTO> peakHours;
    private List<DayStatDTO> daily;
    private List<CourtStatDTO> byCourt;
    private List<SportStatDTO> bySport;

    @Getter
    @Builder
    public static class PeakHourDTO {
        private String hour;
        private long bookings;
    }

    @Getter
    @Builder
    public static class DayStatDTO {
        private String date;
        private long bookings;
        private BigDecimal revenue;
    }

    @Getter
    @Builder
    public static class CourtStatDTO {
        private Long courtId;
        private String courtName;
        private String sportType;
        private String venueName;
        private long bookings;
        private BigDecimal revenue;
    }

    @Getter
    @Builder
    public static class SportStatDTO {
        private String sportType;
        private long bookings;
        private BigDecimal revenue;
    }
}
