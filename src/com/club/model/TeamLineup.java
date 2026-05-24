package com.club.model;

/**
 * Represents a team lineup for a specific match.
 * Contains the starting XI and substitutes with their respective positions.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class TeamLineup {

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

    public String toCsv() {
        return String.join(",",
            safe(lineupId), safe(matchId), safe(teamName), safe(formation),
            safe(startingPlayers), safe(substitutes), safe(tactics), safe(createdAt)
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static TeamLineup fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.split(",", -1);
        TeamLineup tl = new TeamLineup();
        tl.setLineupId(parts.length > 0 ? parts[0] : null);
        tl.setMatchId(parts.length > 1 ? parts[1] : null);
        tl.setTeamName(parts.length > 2 ? parts[2] : null);
        tl.setFormation(parts.length > 3 ? parts[3] : null);
        tl.setStartingPlayers(parts.length > 4 ? parts[4] : null);
        tl.setSubstitutes(parts.length > 5 ? parts[5] : null);
        tl.setTactics(parts.length > 6 ? parts[6] : null);
        tl.setCreatedAt(parts.length > 7 ? parts[7] : null);
        return tl;
    }

    @Override
    public String toString() {
        return String.format("TeamLineup{id='%s', match='%s', team='%s', formation=%s}",
            lineupId, matchId, teamName, formation);
    }
}
