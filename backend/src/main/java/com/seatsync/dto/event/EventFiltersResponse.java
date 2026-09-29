package com.seatsync.dto.event;

import com.seatsync.entity.EventCategory;

import java.util.List;

public record EventFiltersResponse(List<EventCategory> categories, List<String> cities) {
}
