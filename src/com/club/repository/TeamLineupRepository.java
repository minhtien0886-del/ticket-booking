package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link TeamLineup} entities backed by team_lineups.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class TeamLineupRepository extends GenericCsvRepository<TeamLineup> {

    private static final String HEADER = "lineupId,matchId,teamName,formation,startingPlayers,substitutes,tactics,createdAt";

    public TeamLineupRepository(Path dataDir) {
        super(
            dataDir.resolve("team_lineups.csv"),
            "lineupId",
            TeamLineup::getLineupId,
            TeamLineup::fromCsv,
            TeamLineup::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<TeamLineup> findByMatchId(String matchId) {
        return findAll(tl -> matchId.equals(tl.getMatchId()));
    }

    public List<TeamLineup> findByTeamName(String teamName) {
        return findAll(tl -> teamName.equalsIgnoreCase(tl.getTeamName()));
    }

    @Override
    protected TeamLineup parse(String csvLine) {
        return TeamLineup.fromCsv(csvLine);
    }

    @Override
    protected String serialize(TeamLineup entity) {
        return entity.toCsv();
    }
}
