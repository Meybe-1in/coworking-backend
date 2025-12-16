package com.coworking.dto;

import lombok.Data;

@Data
public class RoomAvailabilityResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String location;
    private String imageUrl;
    private boolean available;
}
