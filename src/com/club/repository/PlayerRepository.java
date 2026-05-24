package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link Player} entities backed by players.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class PlayerRepository extends GenericCsvRepository<Player> {

    private static final String HEADER = "id,name,email,position,salary,fitness,injuryStatus,squadNumber,matchesPlayed,goalsScored,assists,joinDate,contractExpiry";

    public PlayerRepository(Path dataDir) {
        super(
            dataDir.resolve("players.csv"),
            "id",
            Player::getId,
            Player::fromCsv,
            Player::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<Player> findByPosition(Position position) {
        return findAll(p -> p.getPosition() == position);
    }

    public List<Player> findHealthyPlayers() {
        return findAll(p -> p.getInjuryStatus() == InjuryStatus.HEALTHY && p.getFitness() >= 70);
    }

    public List<Player> findTopScorers(int limit) {
        List<Player> all = new ArrayList<>(findAll());
        all.sort((a, b) -> Integer.compare(b.getGoalsScored(), a.getGoalsScored()));
        return all.subList(0, Math.min(limit, all.size()));
    }

    public Player findBySquadNumber(int squadNumber) {
        return findOne(p -> p.getSquadNumber() == squadNumber);
    }

    public List<Player> findByFitnessRange(int min, int max) {
        return findAll(p -> p.getFitness() >= min && p.getFitness() <= max);
    }

    public void updateFitness(String playerId, int newFitness) throws IOException {
        Player p = findById(playerId);
        if (p != null) {
            p.setFitness(newFitness);
            save(p);
        }
    }

    @Override
    protected Player parse(String csvLine) {
        return Player.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Player entity) {
        return entity.toCsv();
    }
}
