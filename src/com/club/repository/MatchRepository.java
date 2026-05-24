package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link Match} entities backed by matches.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class MatchRepository extends GenericCsvRepository<Match> {

    private static final String HEADER = "matchId,homeTeam,awayTeam,venue,matchDate,matchTime,status,homeScore,awayScore,attendance,ticketPriceStandard,ticketPriceVip,competition,round,referee";

    public MatchRepository(Path dataDir) {
        super(
            dataDir.resolve("matches.csv"),
            "matchId",
            Match::getMatchId,
            Match::fromCsv,
            Match::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<Match> findByStatus(MatchStatus status) {
        return findAll(m -> m.getStatus() == status);
    }

    public List<Match> findUpcoming() {
        return findAll(m -> m.getStatus().canBookTickets());
    }

    public List<Match> findByHomeTeam(String team) {
        return findAll(m -> team.equalsIgnoreCase(m.getHomeTeam()));
    }

    public List<Match> findByCompetition(String competition) {
        return findAll(m -> competition.equalsIgnoreCase(m.getCompetition()));
    }

    public List<Match> findCompleted() {
        return findAll(m -> m.getStatus() == MatchStatus.COMPLETED);
    }

    @Override
    protected Match parse(String csvLine) {
        return Match.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Match entity) {
        return entity.toCsv();
    }
}
