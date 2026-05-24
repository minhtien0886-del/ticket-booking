package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link LeagueTableEntry} entities backed by league_table.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class LeagueTableRepository extends GenericCsvRepository<LeagueTableEntry> {

    private static final String HEADER = "teamName,played,won,drawn,lost,goalsFor,goalsAgainst,goalDifference,points,season,position";

    public LeagueTableRepository(Path dataDir) {
        super(
            dataDir.resolve("league_table.csv"),
            "teamName",
            e -> e.getTeamName() + "|" + (e.getSeason() != null ? e.getSeason() : ""),
            LeagueTableEntry::fromCsv,
            LeagueTableEntry::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<LeagueTableEntry> findBySeason(String season) {
        return findAll(e -> season.equalsIgnoreCase(e.getSeason()));
    }

    public List<LeagueTableEntry> findSortedByPoints(String season) {
        List<LeagueTableEntry> entries = new ArrayList<>(findBySeason(season));
        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.getPoints(), a.getPoints());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(b.getGoalDifference(), a.getGoalDifference());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getGoalsFor(), a.getGoalsFor());
        });
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setPosition(i + 1);
        }
        return entries;
    }

    @Override
    protected LeagueTableEntry parse(String csvLine) {
        return LeagueTableEntry.fromCsv(csvLine);
    }

    @Override
    protected String serialize(LeagueTableEntry entity) {
        return entity.toCsv();
    }
}
