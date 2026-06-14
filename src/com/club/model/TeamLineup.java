package com.club.model;

/**
 * Represents a team lineup for a specific match.
 * Contains the starting XI and substitutes with their respective positions.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class TeamLineup extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String lineupId;
    private String matchId;
    private String teamName;
    private String formation;
    private String startingPlayers;
    private String substitutes;
    private String tactics;
    private String createdAt;

    public TeamLineup() {}

    public String getLineupId() {
        return lineupId;
    }

    public void setLineupId(String lineupId) {
        this.lineupId = lineupId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getFormation() {
        return formation;
    }

    public void setFormation(String formation) {
        this.formation = formation;
    }

    public String getStartingPlayers() {
        return startingPlayers;
    }

    public void setStartingPlayers(String startingPlayers) {
        this.startingPlayers = startingPlayers;
    }

    public String getSubstitutes() {
        return substitutes;
    }

    public void setSubstitutes(String substitutes) {
        this.substitutes = substitutes;
    }

    public String getTactics() {
        return tactics;
    }

    public void setTactics(String tactics) {
        this.tactics = tactics;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getEntityId() {
        return lineupId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(lineupId), safe(matchId), safe(teamName), safe(formation),
            safe(startingPlayers), safe(substitutes), safe(tactics), safe(createdAt)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static TeamLineup fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        TeamLineup tl = new TeamLineup();
        tl.setLineupId(getField(parts, 0, null));
        tl.setMatchId(getField(parts, 1, null));
        tl.setTeamName(getField(parts, 2, null));
        tl.setFormation(getField(parts, 3, null));
        tl.setStartingPlayers(getField(parts, 4, null));
        tl.setSubstitutes(getField(parts, 5, null));
        tl.setTactics(getField(parts, 6, null));
        tl.setCreatedAt(getField(parts, 7, null));
        return tl;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static TeamLineup fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    @Override
    public String toString() {
        return String.format("TeamLineup{id='%s', match='%s', team='%s', formation=%s}",
            lineupId, matchId, teamName, formation);
    }
}
