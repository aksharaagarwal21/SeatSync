package com.seatsync.service.layout;

import com.seatsync.dto.admin.SectionPricing;
import com.seatsync.entity.SeatSection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SeatLayoutPlannerTest {

    private final SeatLayoutPlanner planner = new SeatLayoutPlanner();
    private final SectionPricing pricing = new SectionPricing(
            new BigDecimal("1999"), new BigDecimal("999"), new BigDecimal("499"));

    @Test
    void generatesOneSeatPerRowAndNumberWithUniqueSeatNumbers() {
        List<SeatLayoutPlanner.PlannedSeat> seats = planner.plan(10, 20, pricing);

        assertThat(seats).hasSize(200);
        assertThat(seats).extracting(SeatLayoutPlanner.PlannedSeat::seatNumber).doesNotHaveDuplicates()
                .contains("A1", "A20", "J1", "J20");
    }

    @Test
    void assignsFrontRowsToVipThenPremiumThenStandard() {
        Map<SeatSection, Long> perSection = planner.plan(10, 20, pricing).stream()
                .collect(Collectors.groupingBy(SeatLayoutPlanner.PlannedSeat::section, Collectors.counting()));

        assertThat(perSection).containsEntry(SeatSection.VIP, 40L)
                .containsEntry(SeatSection.PREMIUM, 80L)
                .containsEntry(SeatSection.STANDARD, 80L);
    }

    @Test
    void pricesSeatsBySection() {
        List<SeatLayoutPlanner.PlannedSeat> seats = planner.plan(10, 20, pricing);

        assertThat(seats.getFirst().price()).isEqualByComparingTo("1999");
        assertThat(seats.getLast().price()).isEqualByComparingTo("499");
    }

    @Test
    void singleRowLayoutIsAllVip() {
        assertThat(planner.plan(1, 8, pricing)).allMatch(seat -> seat.section() == SeatSection.VIP);
    }
}
