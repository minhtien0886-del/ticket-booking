package com.club.model;

import java.util.Objects;

/**
 * Represents a football match within the club's season calendar.
 * Tracks match scheduling, venue, opponent, result, and status throughout
 * the match lifecycle.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class Match {

    private static final long serialVersionUID = 1L;

    private String matchId;
    private String homeTeam;
    private String awayTeam;
    private String venue;
    private String matchDate;
    private String matchTime;
    private MatchStatus status;
    private int homeScore;
    private int awayScore;
    private int attendance;
    private double ticketPriceStandard;
    private double ticketPriceVip;
    private String competition;
    private int round;
    private String referee;

    public Match() {
        this.status = MatchStatus.SCHEDULED;
        this.homeScore = 0;
        this.awayScore = 0;
        this.attendance = 0;
    }

    public Match(String matchId, String homeTeam, String awayTeam, String venue,
                 String matchDate, String matchTime, double ticketPriceStandard, double ticketPriceVip) {
        this.matchId = matchId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.venue = venue;
        this.matchDate = matchDate;
        this.matchTime = matchTime;
        this.status = MatchStatus.SCHEDULED;
        this.homeScore = 0;
        this.awayScore = 0;
        this.ticketPriceStandard = ticketPriceStandard;
        this.ticketPriceVip = ticketPriceVip;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(String matchDate) {
        this.matchDate = matchDate;
    }

    public String getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(String matchTime) {
        this.matchTime = matchTime;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public int getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(int homeScore) {
        this.homeScore = homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(int awayScore) {
        this.awayScore = awayScore;
    }

    public int getAttendance() {
        return attendance;
    }

    public void setAttendance(int attendance) {
        this.attendance = Math.max(0, attendance);
    }

    public double getTicketPriceStandard() {
        return ticketPriceStandard;
    }

    public void setTicketPriceStandard(double ticketPriceStandard) {
        this.ticketPriceStandard = ticketPriceStandard;
    }

    public double getTicketPriceVip() {
        return ticketPriceVip;
    }

    public void setTicketPriceVip(double ticketPriceVip) {
        this.ticketPriceVip = ticketPriceVip;
    }

    public String getCompetition() {
        return competition;
    }

    public void setCompetition(String competition) {
        this.competition = competition;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getReferee() {
        return referee;
    }

    public void setReferee(String referee) {
        this.referee = referee;
    }

    public String getResult() {
        return homeScore + " - " + awayScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return Objects.equals(matchId, match.matchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId);
    }

    @Override
    public String toString() {
        return String.format(
            "Match{id='%s', %s vs %s, date=%s %s, status=%s, result=%s}",
            matchId, homeTeam, awayTeam, matchDate, matchTime, status, getResult()
        );
    }

    public String toCsv() {
        return String.join(",",
            safe(matchId),
            safe(homeTeam),
            safe(awayTeam),
            safe(venue),
            safe(matchDate),
            safe(matchTime),
            status != null ? status.name() : "SCHEDULED",
            String.valueOf(homeScore),
            String.valueOf(awayScore),
            String.valueOf(attendance),
            String.valueOf(ticketPriceStandard),
            String.valueOf(ticketPriceVip),
            safe(competition),
            String.valueOf(round),
            safe(referee)
        );
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static Match fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        Match m = new Match();
        m.setMatchId(parts.length > 0 ? parts[0] : null);
        m.setHomeTeam(parts.length > 1 ? parts[1] : null);
        m.setAwayTeam(parts.length > 2 ? parts[2] : null);
        m.setVenue(parts.length > 3 ? parts[3] : null);
        m.setMatchDate(parts.length > 4 ? parts[4] : null);
        m.setMatchTime(parts.length > 5 ? parts[5] : null);
        try { m.setStatus(MatchStatus.valueOf(parts.length > 6 ? parts[6] : "SCHEDULED")); } catch (Exception e) { /* default */ }
        try { m.setHomeScore(Integer.parseInt(parts.length > 7 ? parts[7] : "0")); } catch (Exception e) { /* default 0 */ }
        try { m.setAwayScore(Integer.parseInt(parts.length > 8 ? parts[8] : "0")); } catch (Exception e) { /* default 0 */ }
        try { m.setAttendance(Integer.parseInt(parts.length > 9 ? parts[9] : "0")); } catch (Exception e) { /* default 0 */ }
        try { m.setTicketPriceStandard(Double.parseDouble(parts.length > 10 ? parts[10] : "0")); } catch (Exception e) { /* default 0 */ }
        try { m.setTicketPriceVip(Double.parseDouble(parts.length > 11 ? parts[11] : "0")); } catch (Exception e) { /* default 0 */ }
        m.setCompetition(parts.length > 12 ? parts[12] : null);
        try { m.setRound(Integer.parseInt(parts.length > 13 ? parts[13] : "0")); } catch (Exception e) { /* default 0 */ }
        m.setReferee(parts.length > 14 ? parts[14] : null);
        return m;
    }

    private static String[] parseCsvLine(String csv) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
