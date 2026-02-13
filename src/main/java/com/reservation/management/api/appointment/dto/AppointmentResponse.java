package com.reservation.management.api.appointment.dto;

public record AppointmentResponse(
        Long appointmentId,
        String patientName,
        String appointmentTime,
        int partySize,
        String status
) {
}
