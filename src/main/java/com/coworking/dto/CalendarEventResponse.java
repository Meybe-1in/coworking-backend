package com.coworking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class CalendarEventResponse {

    private String title;
    private Instant start;
    private Instant end;
    private Long roomId;
}
