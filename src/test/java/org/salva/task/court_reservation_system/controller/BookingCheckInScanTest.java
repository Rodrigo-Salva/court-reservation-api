package org.salva.task.court_reservation_system.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.CheckInRequestDTO;
import org.salva.task.court_reservation_system.dto.response.BookingDetailResponseDTO;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.entity.Venue;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.exception.ResourceNotFoundException;
import org.salva.task.court_reservation_system.security.AccessControlService;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.service.BookingService;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCheckInScanTest {

    @Mock private BookingService bookingService;

    private BookingController controller;

    @BeforeEach
    void setUp() {
        controller = new BookingController(bookingService, new AccessControlService());
    }

    @Test
    void receptionCheckInWithOnlyTheScannedCodeAndTrimsIt() {
        BookingDetailResponseDTO detail = new BookingDetailResponseDTO();
        when(bookingService.findBookingIdByCheckInCode(" abc-123 ")).thenReturn(7L);
        when(bookingService.getVenueIdOfBooking(7L)).thenReturn(1L);
        when(bookingService.getBookingById(7L)).thenReturn(detail);

        var response = controller.checkInByCode(request(" abc-123 "), receptionist(1L));

        assertEquals(detail, response.getBody());
        ArgumentCaptor<CheckInRequestDTO> sent = ArgumentCaptor.forClass(CheckInRequestDTO.class);
        verify(bookingService).checkIn(org.mockito.ArgumentMatchers.eq(7L), sent.capture());
        assertEquals("abc-123", sent.getValue().getCode());
    }

    @Test
    void receptionOfAnotherVenueCannotCheckInTheBooking() {
        when(bookingService.findBookingIdByCheckInCode("abc")).thenReturn(7L);
        when(bookingService.getVenueIdOfBooking(7L)).thenReturn(2L);

        assertThrows(AccessDeniedException.class, () -> controller.checkInByCode(request("abc"), receptionist(1L)));
        verify(bookingService, never()).checkIn(anyLong(), any());
    }

    @Test
    void unknownCodeIsNotFound() {
        when(bookingService.findBookingIdByCheckInCode("nope")).thenThrow(new ResourceNotFoundException("No existe"));

        assertThrows(ResourceNotFoundException.class, () -> controller.checkInByCode(request("nope"), receptionist(1L)));
        verify(bookingService, never()).checkIn(anyLong(), any());
    }

    private CheckInRequestDTO request(String code) {
        CheckInRequestDTO dto = new CheckInRequestDTO();
        dto.setCode(code);
        return dto;
    }

    private CustomUserDetails receptionist(Long venueId) {
        User user = new User();
        user.setId(9L);
        user.setRole(Role.RECEPTIONIST);
        user.setVenue(Venue.builder().id(venueId).name("V" + venueId).build());
        return new CustomUserDetails(user);
    }
}
