package com.club.model;

import java.util.Objects;

/**
 * Represents an entry in the league standings table.
 * Tracks each team's performance metrics throughout the season.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class LeagueTableEntry extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String teamName;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int points;
    private String season;
    private int position;

    public LeagueTableEntry() {
        this.played = 0;
        this.won = 0;
        this.drawn = 0;
        this.lost = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
        this.goalDifference = 0;
        this.points = 0;
    }

    public LeagueTableEntry(String teamName, String season) {
        this();
        this.teamName = teamName;
        this.season = season;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getPlayed() {
        return played;
    }

    public void setPlayed(int played) {
        this.played = played;
    }

    public int getWon() {
        return won;
    }

    public void setWon(int won) {
        this.won = won;
    }

    public int getDrawn() {
        return drawn;
    }

    public void setDrawn(int drawn) {
        this.drawn = drawn;
    }

    public int getLost() {
        return lost;
    }

    public void setLost(int lost) {
        this.lost = lost;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public void setGoalsFor(int goalsFor) {
        this.goalsFor = goalsFor;
        recalculate();
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
        recalculate();
    }

    public int getGoalDifference() {
        return goalDifference;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private void recalculate() {
        this.goalDifference = this.goalsFor - this.goalsAgainst;
        this.points = (this.won * 3) + this.drawn;
        this.played = this.won + this.drawn + this.lost;
    }

    public void recordWin(int goalsFor, int goalsAgainst) {
        this.won++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        recalculate();
    }

    public void recordDraw(int goalsFor, int goalsAgainst) {
        this.drawn++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        recalculate();
    }

    public void recordLoss(int goalsFor, int goalsAgainst) {
        this.lost++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        recalculate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeagueTableEntry that = (LeagueTableEntry) o;
        return Objects.equals(teamName, that.teamName) &&
               Objects.equals(season, that.season);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamName, season);
    }

    @Override
    public String toString() {
        return String.format(
            "LeagueTableEntry{pos=%d, team='%s', P=%d, W=%d, D=%d, L=%d, GF=%d, GA=%d, GD=%+d, pts=%d}",
            position, teamName, played, won, drawn, lost, goalsFor, goalsAgainst, goalDifference, points
        );
    }

    @Override
    public String getEntityId() {
        return teamName + "_" + season;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(teamName),
            String.valueOf(played),
            String.valueOf(won),
            String.valueOf(drawn),
            String.valueOf(lost),
            String.valueOf(goalsFor),
            String.valueOf(goalsAgainst),
            String.valueOf(goalDifference),
            String.valueOf(points),
            safe(season),
            String.valueOf(position)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static LeagueTableEntry fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        LeagueTableEntry e = new LeagueTableEntry();
        e.setTeamName(getField(parts, 0, null));
        e.setPlayed(getIntField(parts, 1, 0));
        e.setWon(getIntField(parts, 2, 0));
        e.setDrawn(getIntField(parts, 3, 0));
        e.setLost(getIntField(parts, 4, 0));
        e.setGoalsFor(getIntField(parts, 5, 0));
        e.setGoalsAgainst(getIntField(parts, 6, 0));
        // index 7 is goalDifference (calculated)
        e.setPoints(getIntField(parts, 8, 0));
        e.setSeason(getField(parts, 9, null));
        e.setPosition(getIntField(parts, 10, 0));
        return e;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static LeagueTableEntry fromCsv(String csv) {
        return fromCsvLine(csv);
    }
}
