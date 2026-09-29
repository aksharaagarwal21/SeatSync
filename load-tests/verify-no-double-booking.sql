-- Run after a load test. Every query must return zero rows.

-- 1. No seat belongs to more than one confirmed booking.
SELECT bs.seat_id, COUNT(*) AS confirmed_bookings
FROM booking_seats bs
JOIN bookings b ON b.id = bs.booking_id
WHERE b.status = 'CONFIRMED'
GROUP BY bs.seat_id
HAVING COUNT(*) > 1;

-- 2. Every BOOKED seat is backed by exactly one active line of a confirmed booking.
SELECT s.id AS seat_id, s.seat_number, COUNT(bs.id) AS active_lines
FROM seats s
LEFT JOIN booking_seats bs ON bs.seat_id = s.id AND bs.active
LEFT JOIN bookings b ON b.id = bs.booking_id AND b.status = 'CONFIRMED'
WHERE s.status = 'BOOKED'
GROUP BY s.id, s.seat_number
HAVING COUNT(b.id) <> 1;

-- 3. No active booking line points at a seat that is not BOOKED.
SELECT bs.id AS booking_seat_id, bs.seat_id, s.status
FROM booking_seats bs
JOIN bookings b ON b.id = bs.booking_id
JOIN seats s ON s.id = bs.seat_id
WHERE bs.active AND b.status = 'CONFIRMED' AND s.status <> 'BOOKED';

-- 4. Booking totals equal the sum of their seat prices.
SELECT b.id, b.total_amount, SUM(bs.price) AS seat_total
FROM bookings b
JOIN booking_seats bs ON bs.booking_id = b.id
GROUP BY b.id, b.total_amount
HAVING b.total_amount <> SUM(bs.price);
