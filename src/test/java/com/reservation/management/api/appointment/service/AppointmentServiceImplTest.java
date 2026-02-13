package com.reservation.management.api.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reservation.management.api.appointment.domain.Appointment;
import com.reservation.management.api.appointment.domain.AppointmentStatus;
import com.reservation.management.api.appointment.dto.AppointmentCreateRequest;
import com.reservation.management.api.appointment.dto.AppointmentResponse;
import com.reservation.management.api.appointment.repository.AppointmentRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(appointmentRepository);
    }

    @Test
    @DisplayName("예약 생성 성공")
    void createAppointment_success_savesAsRequested() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        AppointmentCreateRequest request = new AppointmentCreateRequest("홍길동", futureTime.toString(), 2);

        when(appointmentRepository.existsByAppointmentTimeAndStatusIn(
                eq(futureTime),
                eq(EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED))
        )).thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertEquals(1L, response.appointmentId());
        assertEquals("홍길동", response.patientName());
        assertEquals(2, response.partySize());
        assertEquals("REQUESTED", response.status());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("예약 생성 실패: 과거 시간")
    void createAppointment_fail_whenAppointmentTimeIsPast() {
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        AppointmentCreateRequest request = new AppointmentCreateRequest("홍길동", pastTime.toString(), 2);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(exception.getMessage().contains("예약 시간"));
        verify(appointmentRepository, never()).existsByAppointmentTimeAndStatusIn(any(), any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("예약 생성 실패: 동일 시간 중복")
    void createAppointment_fail_whenActiveAppointmentExistsAtSameTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(3);
        AppointmentCreateRequest request = new AppointmentCreateRequest("홍길동", futureTime.toString(), 2);

        when(appointmentRepository.existsByAppointmentTimeAndStatusIn(
                eq(futureTime),
                eq(EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED))
        )).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(exception.getMessage().contains("동일 시간대"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("예약 생성 실패: partySize가 1 미만")
    void createAppointment_fail_whenPartySizeLessThanOne() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(3);
        AppointmentCreateRequest request = new AppointmentCreateRequest("홍길동", futureTime.toString(), 0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertEquals("예약 인원은 1명 이상이어야 합니다.", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("예약 취소 성공")
    void cancelAppointment_success_whenStatusIsRequested() {
        Appointment reservation = createAppointment(10L, AppointmentStatus.REQUESTED, LocalDateTime.now().plusDays(1));
        LocalDateTime beforeCancelUpdateTime = reservation.getUpdatedAt();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(10L);

        assertEquals("CANCELED", response.status());
        assertEquals(AppointmentStatus.CANCELED, reservation.getStatus());
        assertNotNull(reservation.getCancelReason());
        assertTrue(reservation.getUpdatedAt().isAfter(beforeCancelUpdateTime));
        verify(appointmentRepository).save(reservation);
    }

    @ParameterizedTest(name = "예약 취소 실패: {0} 상태")
    @MethodSource("nonCancelableStatuses")
    void cancelAppointment_fail_whenStatusIsFinalized(AppointmentStatus finalizedStatus) {
        Appointment reservation = createAppointment(20L, finalizedStatus, LocalDateTime.now().plusDays(1));
        when(appointmentRepository.findById(20L)).thenReturn(Optional.of(reservation));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> appointmentService.cancelAppointment(20L)
        );

        assertTrue(exception.getMessage().contains("취소"));
        verify(appointmentRepository, never()).save(any());
    }

    private static Stream<Arguments> nonCancelableStatuses() {
        return Stream.of(
                Arguments.of(AppointmentStatus.CANCELED),
                Arguments.of(AppointmentStatus.COMPLETED)
        );
    }

    private Appointment createAppointment(Long id, AppointmentStatus status, LocalDateTime appointmentTime) {
        Appointment reservation = new Appointment();
        ReflectionTestUtils.setField(reservation, "id", id);
        reservation.setAppointmentNumber("RSV-TEST-" + id);
        reservation.setPatientName("테스트");
        reservation.setCustomerPhone("010-0000-0000");
        reservation.setCustomerEmail("test@example.com");
        reservation.setAppointmentTime(appointmentTime);
        reservation.setPartySize(2);
        reservation.setStatus(status);
        reservation.setCancelReason(null);
        reservation.setCreatedAt(LocalDateTime.now().minusHours(2));
        reservation.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return reservation;
    }
}