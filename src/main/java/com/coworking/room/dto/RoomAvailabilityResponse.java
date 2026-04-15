package com.coworking.room.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomAvailabilityResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private BigDecimal price;
    private String location;
    private String imageUrl;
    private boolean available;
}
