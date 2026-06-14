package com.club.model;

import java.util.Objects;

/**
 * Concrete {@link Person} subclass representing a football player in the club system.
 * Extends the base person entity with football-specific attributes including position,
 * salary, fitness level, and injury status.
 *
 * <p>Instances of this class represent active squad members tracked for team selection,
 * performance analytics, salary management, and match lineups.</p>
 *
 * <p>Fitness level is an integer from 0-100 where 100 represents peak physical condition.
 * A player's fitness directly impacts their eligibility for team selection.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see Person
 * @see Position
 * @see InjuryStatus
 */
public class Player extends Person {

    private static final long serialVersionUID = 1L;

    /** Primary playing position on the pitch. */
    private Position position;

    /** Player's monthly salary amount. */
    private double salary;

    /** Physical fitness level from 0 (incapacitated) to 100 (peak condition). */
    private int fitness; // 0-100

    /** Current injury status tracking the player's availability. */
    private InjuryStatus injuryStatus;

    /** Jersey squad number assigned to the player. */
    private int squadNumber;

    /** Number of matches played this season. */
    private int matchesPlayed;

    /** Total goals scored across all competitions. */
    private int goalsScored;

    /** Total assists made across all competitions. */
    private int assists;

    /** Date the player joined the club (ISO format). */
    private String joinDate;

    /** Contract expiration date (ISO format). */
    private String contractExpiry;

    /**
     * Default constructor — generates a UUID and sets default injury status.
     */
    public Player() {
        super();
        this.injuryStatus = InjuryStatus.HEALTHY;
        this.fitness = 100;
        this.matchesPlayed = 0;
        this.goalsScored = 0;
        this.assists = 0;
    }

    /**
     * Full constructor for creating a player with all attributes.
     *
     * @param id           unique identifier
     * @param name         full name
     * @param email        contact email
     * @param position     playing position
     * @param salary       monthly salary
     * @param fitness      fitness level (0-100)
     * @param injuryStatus current injury status
     * @param squadNumber  jersey number
     */
    public Player(String id, String name, String email, Position position,
                  double salary, int fitness, InjuryStatus injuryStatus, int squadNumber) {
        super(id, name, email);
        this.position = position;
        setSalary(salary);
        setFitness(fitness);
        this.injuryStatus = injuryStatus != null ? injuryStatus : InjuryStatus.HEALTHY;
        this.squadNumber = squadNumber;
        this.matchesPlayed = 0;
        this.goalsScored = 0;
        this.assists = 0;
    }

    /**
     * Simplified constructor for creating a player with essential attributes.
     *
     * @param name         full name
     * @param email        contact email
     * @param position     playing position
     * @param salary       monthly salary
     * @param squadNumber  jersey number
     */
    public Player(String name, String email, Position position, double salary, int squadNumber) {
        super(name, email);
        this.position = position;
        setSalary(salary);
        this.fitness = 100;
        this.injuryStatus = InjuryStatus.HEALTHY;
        this.squadNumber = squadNumber;
    }

    @Override
    public String getPersonType() {
        return "PLAYER";
    }

    // ============ GETTERS AND SETTERS ============

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Player position cannot be null");
        }
        this.position = position;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative: " + salary);
        }
        this.salary = salary;
    }

    public int getFitness() {
        return fitness;
    }

    public void setFitness(int fitness) {
        if (fitness < 0 || fitness > 100) {
            throw new IllegalArgumentException("Fitness must be between 0 and 100, got: " + fitness);
        }
        this.fitness = fitness;
    }

    public InjuryStatus getInjuryStatus() {
        return injuryStatus;
    }

    public void setInjuryStatus(InjuryStatus injuryStatus) {
        this.injuryStatus = injuryStatus != null ? injuryStatus : InjuryStatus.HEALTHY;
    }

    public int getSquadNumber() {
        return squadNumber;
    }

    public void setSquadNumber(int squadNumber) {
        if (squadNumber < 1 || squadNumber > 99) {
            throw new IllegalArgumentException("Squad number must be between 1 and 99");
        }
        this.squadNumber = squadNumber;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        if (matchesPlayed < 0) {
            throw new IllegalArgumentException("Matches played cannot be negative");
        }
        this.matchesPlayed = matchesPlayed;
    }

    public int getGoalsScored() {
        return goalsScored;
    }

    public void setGoalsScored(int goalsScored) {
        if (goalsScored < 0) {
            throw new IllegalArgumentException("Goals scored cannot be negative");
        }
        this.goalsScored = goalsScored;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        if (assists < 0) {
            throw new IllegalArgumentException("Assists cannot be negative");
        }
        this.assists = assists;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public String getContractExpiry() {
        return contractExpiry;
    }

    public void setContractExpiry(String contractExpiry) {
        this.contractExpiry = contractExpiry;
    }

    // ============ BUSINESS METHODS ============

    /**
     * Checks whether the player is eligible for team selection.
     * A player is eligible if they are healthy and fitness is above the threshold.
     *
     * @param minFitness the minimum fitness threshold for selection
     * @return true if the player can be selected for a match
     */
    public boolean isEligibleForSelection(int minFitness) {
        return injuryStatus.isAvailable() && this.fitness >= minFitness;
    }

    /**
     * Records a goal scored by this player in a match.
     *
     * @param goals number of goals scored (typically 1)
     */
    public void recordGoal(int goals) {
        if (goals > 0) {
            this.goalsScored += goals;
            this.matchesPlayed++;
        }
    }

    /**
     * Records an assist made by this player in a match.
     *
     * @param assists number of assists (typically 1)
     */
    public void recordAssist(int assists) {
        if (assists > 0) {
            this.assists += assists;
        }
    }

    /**
     * Simulates a match appearance, incrementing the played counter.
     */
    public void recordMatchPlayed() {
        this.matchesPlayed++;
    }

    /**
     * Updates fitness after a match. Fitness degrades slightly after playing.
     *
     * @param minutesPlayed how many minutes the player was on the pitch
     */
    public void degradeFitnessAfterMatch(int minutesPlayed) {
        int degradation = (int) (minutesPlayed / 10.0);
        this.fitness = Math.max(0, this.fitness - degradation);
    }

    /**
     * Calculates the player's market value based on performance metrics.
     *
     * @return estimated market value in currency units
     */
    public double calculateMarketValue() {
        double baseValue = salary * 12 * 5;
        double performanceBonus = (goalsScored * 50000) + (assists * 30000);
        double fitnessFactor = fitness / 100.0;
        return (baseValue + performanceBonus) * fitnessFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Player player = (Player) o;
        return squadNumber == player.squadNumber &&
               Objects.equals(position, player.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), position, squadNumber);
    }

    @Override
    public String toString() {
        return String.format(
            "Player{id='%s', name='%s', position=%s, squadNumber=%d, salary=%.2f, " +
            "fitness=%d, injuryStatus=%s, matchesPlayed=%d, goals=%d, assists=%d}",
            id, name, position, squadNumber, salary, fitness, injuryStatus,
            matchesPlayed, goalsScored, assists
        );
    }

    /**
     * Returns a CSV representation of this player for persistence.
     *
     * @return comma-separated value string
     */
    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(id),
            safe(name),
            safe(email),
            position != null ? position.name() : "",
            String.valueOf(salary),
            String.valueOf(fitness),
            injuryStatus != null ? injuryStatus.name() : "HEALTHY",
            String.valueOf(squadNumber),
            String.valueOf(matchesPlayed),
            String.valueOf(goalsScored),
            String.valueOf(assists),
            safe(joinDate),
            safe(contractExpiry)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    /**
     * Parses a CSV line into a Player object.
     *
     * @param csv the comma-separated line
     * @return a new Player instance
     */
    public static Player fromCsvLine(String csv) {
        String[] parts = parseCsvLine(csv);
        if (parts.length < 3) return null;
        Player p = new Player();
        p.setId(parts[0]);
        p.setName(parts[1]);
        p.setEmail(parts[2]);
        try { p.setPosition(Position.valueOf(getField(parts, 3, "STRIKER"))); } catch (Exception e) { /* default */ }
        p.setSalary(getDoubleField(parts, 4, 0.0));
        p.setFitness(getIntField(parts, 5, 100));
        try { p.setInjuryStatus(InjuryStatus.valueOf(getField(parts, 6, "HEALTHY"))); } catch (Exception e) { /* default */ }
        p.setSquadNumber(getIntField(parts, 7, 1));
        p.setMatchesPlayed(getIntField(parts, 8, 0));
        p.setGoalsScored(getIntField(parts, 9, 0));
        p.setAssists(getIntField(parts, 10, 0));
        p.setJoinDate(getField(parts, 11, null));
        p.setContractExpiry(getField(parts, 12, null));
        return p;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static Player fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
