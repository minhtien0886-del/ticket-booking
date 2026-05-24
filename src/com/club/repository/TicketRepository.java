package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link Ticket} entities backed by tickets.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class TicketRepository extends GenericCsvRepository<Ticket> {

    private static final String HEADER = "ticketId,matchId,fanId,seatId,category,price,purchaseDate,matchDate,checkedIn,checkedInTime,transactionId";

    public TicketRepository(Path dataDir) {
        super(
            dataDir.resolve("tickets.csv"),
            "ticketId",
            Ticket::getTicketId,
            Ticket::fromCsv,
            Ticket::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<Ticket> findByMatchId(String matchId) {
        return findAll(t -> matchId.equals(t.getMatchId()));
    }

    public List<Ticket> findByFanId(String fanId) {
        return findAll(t -> fanId.equals(t.getFanId()));
    }

    public List<Ticket> findBySeatId(String seatId) {
        return findAll(t -> seatId.equals(t.getSeatId()));
    }

    public List<Ticket> findByMatchAndSeat(String matchId, String seatId) {
        return findAll(t -> matchId.equals(t.getMatchId()) && seatId.equals(t.getSeatId()));
    }

    public List<Ticket> findCheckedInByMatch(String matchId) {
        return findAll(t -> matchId.equals(t.getMatchId()) && t.isCheckedIn());
    }

    @Override
    protected Ticket parse(String csvLine) {
        return Ticket.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Ticket entity) {
        return entity.toCsv();
    }
}
