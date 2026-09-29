package com.seatsync.dto.admin;

import com.seatsync.entity.SeatSection;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SectionPricing(
        @NotNull(message = "VIP price is required")
        @Positive(message = "VIP price must be positive")
        @DecimalMax("100000")
        @Digits(integer = 6, fraction = 2)
        BigDecimal vip,

        @NotNull(message = "Premium price is required")
        @Positive(message = "Premium price must be positive")
        @DecimalMax("100000")
        @Digits(integer = 6, fraction = 2)
        BigDecimal premium,

        @NotNull(message = "Standard price is required")
        @Positive(message = "Standard price must be positive")
        @DecimalMax("100000")
        @Digits(integer = 6, fraction = 2)
        BigDecimal standard
) {

    public BigDecimal priceFor(SeatSection section) {
        return switch (section) {
            case VIP -> vip;
            case PREMIUM -> premium;
            case STANDARD -> standard;
        };
    }
}
