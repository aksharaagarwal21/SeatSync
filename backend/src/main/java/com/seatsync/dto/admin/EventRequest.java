package com.seatsync.dto.admin;

import com.seatsync.entity.EventCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventRequest(
        @NotBlank(message = "Event name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 2000)
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotBlank(message = "Venue is required")
        @Size(max = 150)
        String venue,

        @NotBlank(message = "City is required")
        @Size(max = 80)
        String city,

        @NotNull(message = "Date is required")
        LocalDate eventDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Image URL must start with http:// or https://")
        String imageUrl,

        boolean bookingOpen,

        @Min(value = 1, message = "At least 1 row is required")
        @Max(value = 26, message = "At most 26 rows (A–Z) are supported")
        int rows,

        @Min(value = 1, message = "At least 1 seat per row is required")
        @Max(value = 40, message = "At most 40 seats per row are supported")
        int seatsPerRow,

        @NotNull(message = "Pricing is required")
        @Valid
        SectionPricing pricing
) {
}
