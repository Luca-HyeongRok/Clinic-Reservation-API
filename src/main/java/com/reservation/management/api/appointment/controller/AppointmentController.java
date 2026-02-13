package com.reservation.management.api.appointment.controller;

import com.reservation.management.api.appointment.dto.AppointmentCreateRequest;
import com.reservation.management.api.appointment.dto.AppointmentResponse;
import com.reservation.management.api.appointment.service.AppointmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody AppointmentCreateRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable Long appointmentId) {
        AppointmentResponse response = appointmentService.getAppointment(appointmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAppointments() {
        List<AppointmentResponse> responses = appointmentService.getAppointments();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long appointmentId) {
        AppointmentResponse response = appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(response);
    }
}