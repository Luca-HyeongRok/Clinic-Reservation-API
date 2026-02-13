package com.reservation.management.api.appointment.service;

import com.reservation.management.api.appointment.domain.Appointment;
import com.reservation.management.api.appointment.domain.AppointmentStatus;
import com.reservation.management.api.appointment.dto.AppointmentCreateRequest;
import com.reservation.management.api.appointment.dto.AppointmentResponse;
import com.reservation.management.api.appointment.repository.AppointmentRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private static final EnumSet<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED);

    private static final EnumSet<AppointmentStatus> CANNOT_CANCEL_STATUSES =
            EnumSet.of(AppointmentStatus.CANCELED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW);

    private static final String DEFAULT_CANCEL_REASON = "사용자 요청 취소";

    private final AppointmentRepository appointmentRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        validateCreateRequest(request);

        LocalDateTime appointmentTime = parseAppointmentTime(request.appointmentTime());

        if (!appointmentTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("예약 시간은 현재 시각 이후여야 합니다.");
        }

        boolean duplicated = appointmentRepository.existsByAppointmentTimeAndStatusIn(appointmentTime, ACTIVE_STATUSES);
        if (duplicated) {
            throw new IllegalStateException("동일 시간대에 이미 활성 예약이 존재합니다.");
        }

        Appointment reservation = new Appointment();
        reservation.setAppointmentNumber(generateAppointmentNumber());
        reservation.setPatientName(request.patientName().trim());
        reservation.setCustomerPhone("UNKNOWN");
        reservation.setCustomerEmail(null);
        reservation.setAppointmentTime(appointmentTime);
        reservation.setPartySize(request.partySize());
        reservation.setStatus(AppointmentStatus.REQUESTED);
        reservation.setCancelReason(null);

        LocalDateTime now = LocalDateTime.now();
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);

        Appointment saved = appointmentRepository.save(reservation);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(Long appointmentId) {
        Appointment reservation = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다. id=" + appointmentId));
        return toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment reservation = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다. id=" + appointmentId));

        AppointmentStatus currentStatus = reservation.getStatus();

        if (CANNOT_CANCEL_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException("이미 종결된 예약은 취소할 수 없습니다. status=" + currentStatus);
        }

        if (!ACTIVE_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다. status=" + currentStatus);
        }

        reservation.setStatus(AppointmentStatus.CANCELED);
        reservation.setCancelReason(DEFAULT_CANCEL_REASON);
        reservation.setUpdatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(reservation);
        return toResponse(saved);
    }

    private void validateCreateRequest(AppointmentCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("예약 요청은 필수입니다.");
        }

        if (request.patientName() == null || request.patientName().trim().isEmpty()) {
            throw new IllegalArgumentException("예약자 이름은 필수입니다.");
        }

        if (request.appointmentTime() == null || request.appointmentTime().trim().isEmpty()) {
            throw new IllegalArgumentException("예약 시간은 필수입니다.");
        }

        if (request.partySize() == null || request.partySize() < 1) {
            throw new IllegalArgumentException("예약 인원은 1명 이상이어야 합니다.");
        }
    }

    private LocalDateTime parseAppointmentTime(String appointmentTimeText) {
        try {
            return LocalDateTime.parse(appointmentTimeText.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("예약 시간 형식이 올바르지 않습니다. ISO-8601 형식을 사용하세요.", e);
        }
    }

    private String generateAppointmentNumber() {
        return "RSV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private AppointmentResponse toResponse(Appointment reservation) {
        return new AppointmentResponse(
                reservation.getId(),
                reservation.getPatientName(),
                reservation.getAppointmentTime().toString(),
                reservation.getPartySize(),
                reservation.getStatus().name()
        );
    }
}