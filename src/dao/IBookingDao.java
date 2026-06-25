package dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import model.Booking;

/** Defines data access operations for table bookings. */
public interface IBookingDao {

    List<Booking> list();

    Optional<Booking> pendingForTable(String maBan);

    String create(String maKH, String maBan, LocalDateTime start, LocalDateTime end);

    String createForGuest(
            String phone,
            String name,
            String maBan,
            LocalDateTime start,
            LocalDateTime end);

    void cancel(String maBooking);
}
