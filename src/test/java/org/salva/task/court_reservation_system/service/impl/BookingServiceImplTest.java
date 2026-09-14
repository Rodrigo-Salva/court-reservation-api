package org.salva.task.court_reservation_system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salva.task.court_reservation_system.dto.request.BookingRequestDTO;
import org.salva.task.court_reservation_system.dto.response.BookingResponseDTO;
import org.salva.task.court_reservation_system.entity.Booking;
import org.salva.task.court_reservation_system.entity.Court;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.BookingStatus;
import org.salva.task.court_reservation_system.enums.MembershipType;
import org.salva.task.court_reservation_system.mapper.BookingMapper;
import org.salva.task.court_reservation_system.repository.BookingRepository;
import org.salva.task.court_reservation_system.repository.CourtRepository;
import org.salva.task.court_reservation_system.repository.UserPackageRepository;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.service.impl.BookingServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourtRepository courtRepository;
    @Mock
    private UserPackageRepository userPackageRepository;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Court testCourt;
    private BookingRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Perez");
        testUser.setMembershipType(MembershipType.VIP);
        testUser.setActive(true);

        testCourt = new Court();
        testCourt.setId(1);
        testCourt.setName("Cancha 1");
        testCourt.setPriceBaseHour(BigDecimal.valueOf(50.0));
        testCourt.setActive(true);

        requestDTO = new BookingRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setCourtId(1L);
        requestDTO.setBookingDate(LocalDate.now().plusDays(5)); // Futuro
        requestDTO.setStartTime(LocalTime.of(10, 0));
        requestDTO.setEndTime(LocalTime.of(12, 0));
        requestDTO.setUsesPackage(false);
    }

    @Test
    @DisplayName("Debe crear una reserva exitosamente cuando hay disponibilidad")
    void createBooking_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(testCourt));
        
        when(bookingRepository.existsOverlappingBooking(
                eq(1L), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)
        )).thenReturn(false);

        Booking mappedBooking = new Booking();
        mappedBooking.setBookingDate(requestDTO.getBookingDate());
        mappedBooking.setStartTime(requestDTO.getStartTime());
        mappedBooking.setEndTime(requestDTO.getEndTime());
        mappedBooking.setCourt(testCourt);
        mappedBooking.setUser(testUser);
        
        when(bookingMapper.toEntity(any(BookingRequestDTO.class))).thenReturn(mappedBooking);

        Booking savedBooking = new Booking();
        savedBooking.setId(100L);
        savedBooking.setTotalPrice(BigDecimal.valueOf(100.0));
        
        BookingResponseDTO responseDTO = new BookingResponseDTO();
        responseDTO.setId(100L);
        responseDTO.setStatus(BookingStatus.CONFIRMADA);

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponseDTO(any(Booking.class))).thenReturn(responseDTO);

        // Act
        BookingResponseDTO result = bookingService.createBooking(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(BookingStatus.CONFIRMADA, result.getStatus());
        
        verify(userRepository).findById(1L);
        verify(courtRepository).findById(1L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando hay solapamiento de horario")
    void createBooking_OverlapException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(testCourt));
        
        when(bookingRepository.existsOverlappingBooking(
                eq(1L), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)
        )).thenReturn(true);

        // Act & Assert
        assertThrows(org.salva.task.court_reservation_system.exception.BusinessException.class, () -> {
            bookingService.createBooking(requestDTO);
        });

        // Verificamos que nunca se llame al guardado
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
