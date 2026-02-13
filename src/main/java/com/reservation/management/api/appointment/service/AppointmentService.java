package com.reservation.management.api.appointment.service;

import com.reservation.management.api.appointment.dto.AppointmentCreateRequest;
import com.reservation.management.api.appointment.dto.AppointmentResponse;
import java.util.List;

/**
 * 예약 유스케이스 계약입니다.
 */
public interface AppointmentService {

    /**
     * 클라이언트 요청으로 새 예약을 생성합니다.
     */
    AppointmentResponse createAppointment(AppointmentCreateRequest request);

    /**
     * 예약 ID로 단건 예약을 조회합니다.
     */
    AppointmentResponse getAppointment(Long appointmentId);

    /**
     * 전체 예약 목록을 조회합니다.
     */
    List<AppointmentResponse> getAppointments();

    /**
     * 기존 예약을 취소합니다.
     */
    AppointmentResponse cancelAppointment(Long appointmentId);
}
