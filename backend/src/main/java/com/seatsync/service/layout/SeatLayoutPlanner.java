package com.seatsync.service.layout;

import com.seatsync.dto.admin.SectionPricing;
import com.seatsync.entity.SeatSection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a rows × seats-per-row layout into concrete seats. Front rows (~20%) are VIP, the next
 * ~40% are Premium and the rest Standard, mirroring a typical auditorium price gradient.
 */
@Component
public class SeatLayoutPlanner {

    public List<PlannedSeat> plan(int rows, int seatsPerRow, SectionPricing pricing) {
        List<PlannedSeat> seats = new ArrayList<>(rows * seatsPerRow);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            String row = rowLabel(rowIndex);
            SeatSection section = sectionFor(rowIndex, rows);
            BigDecimal price = pricing.priceFor(section);
            for (int number = 1; number <= seatsPerRow; number++) {
                seats.add(new PlannedSeat(row, number, row + number, section, price));
            }
        }
        return seats;
    }

    static SeatSection sectionFor(int rowIndex, int totalRows) {
        int vipRows = Math.max(1, Math.round(totalRows * 0.2f));
        int premiumRows = Math.round(totalRows * 0.4f);
        if (rowIndex < vipRows) {
            return SeatSection.VIP;
        }
        return rowIndex < vipRows + premiumRows ? SeatSection.PREMIUM : SeatSection.STANDARD;
    }

    static String rowLabel(int rowIndex) {
        return String.valueOf((char) ('A' + rowIndex));
    }

    public record PlannedSeat(String row, int number, String seatNumber, SeatSection section, BigDecimal price) {
    }
}
