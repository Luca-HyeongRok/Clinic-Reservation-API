package com.reservation.management.api.appointment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AppointmentCreateRequest(
        String patientName,
        String appointmentTime,
        @NotNull
        @Min(1)
        Long doctorId,
        @NotNull
        @Min(1)
        Integer partySize
) {
}
