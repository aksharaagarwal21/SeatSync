#!/usr/bin/env bash
# Checks the database for double bookings and inconsistent seat state.
# Exits non-zero if any check returns rows. Works against the docker compose stack by default;
# set PSQL to point at another database, e.g. PSQL="psql postgresql://user:pass@host/db".
set -euo pipefail

cd "$(dirname "$0")"
DB_USER="${DATABASE_USERNAME:-seatsync}"
DB_NAME="${POSTGRES_DB:-seatsync}"
PSQL="${PSQL:-docker compose -f ../docker-compose.yml exec -T postgres psql -U $DB_USER -d $DB_NAME}"

run() {
  $PSQL -v ON_ERROR_STOP=1 -At -c "$1"
}

declare -A CHECKS=(
  ["seats in more than one confirmed booking"]="SELECT count(*) FROM (SELECT bs.seat_id FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id WHERE b.status = 'CONFIRMED' GROUP BY bs.seat_id HAVING count(*) > 1) t"
  ["BOOKED seats without exactly one confirmed booking"]="SELECT count(*) FROM (SELECT s.id FROM seats s LEFT JOIN booking_seats bs ON bs.seat_id = s.id AND bs.active LEFT JOIN bookings b ON b.id = bs.booking_id AND b.status = 'CONFIRMED' WHERE s.status = 'BOOKED' GROUP BY s.id HAVING count(b.id) <> 1) t"
  ["confirmed booking lines on non-BOOKED seats"]="SELECT count(*) FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id JOIN seats s ON s.id = bs.seat_id WHERE bs.active AND b.status = 'CONFIRMED' AND s.status <> 'BOOKED'"
  ["bookings whose total differs from seat prices"]="SELECT count(*) FROM (SELECT b.id FROM bookings b JOIN booking_seats bs ON bs.booking_id = b.id GROUP BY b.id, b.total_amount HAVING b.total_amount <> sum(bs.price)) t"
)

failed=0
for name in "${!CHECKS[@]}"; do
  count="$(run "${CHECKS[$name]}" | tr -d '[:space:]')"
  if [[ "$count" == "0" ]]; then
    printf '  PASS  %s\n' "$name"
  else
    printf '  FAIL  %s: %s rows\n' "$name" "$count"
    failed=1
  fi
done

printf '\nConfirmed bookings: %s   Booked seats: %s\n' \
  "$(run "SELECT count(*) FROM bookings WHERE status = 'CONFIRMED'")" \
  "$(run "SELECT count(*) FROM seats WHERE status = 'BOOKED'")"

if [[ $failed -ne 0 ]]; then
  echo "Double-booking verification FAILED" >&2
  exit 1
fi
echo "No double bookings found."
