package com.coworking.admin.controller;


import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.service.AdminService;
import com.coworking.dto.common.ApiResponseDto;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http:/localhost:5173")
public class AdminController {

    private final AdminService adminService;

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                      Dashboard stats
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats(){
        return  ResponseEntity.ok(
                adminService.getStats()
        );
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                     All reservations
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservations(){
        return ResponseEntity.ok(
                adminService.getAllReservations()
        );
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                       All payments
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(){
        return ResponseEntity.ok(
                adminService.getAllPayments()
        );
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                   Cancel reservation
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    @PatchMapping("/reservations/{id}/cancel")
    public ResponseEntity<ApiResponseDto<String>> cancelReservation(@PathVariable Long id){
        adminService.cancelReservation(id);

        return ResponseEntity.ok(
                new ApiResponseDto<>(
                        true,
                        "Reserva cancelada correctamente",
                        null
                )
        );
    }


}
