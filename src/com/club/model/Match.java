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
public class Match extends BaseEntity {

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

    @Override
    public String getEntityId() {
        return matchId;
    }

    @Override
    public String toCsvLine() {
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

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static Match fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        Match m = new Match();
        m.setMatchId(getField(parts, 0, null));
        m.setHomeTeam(getField(parts, 1, null));
        m.setAwayTeam(getField(parts, 2, null));
        m.setVenue(getField(parts, 3, null));
        m.setMatchDate(getField(parts, 4, null));
        m.setMatchTime(getField(parts, 5, null));
        try { m.setStatus(MatchStatus.valueOf(getField(parts, 6, "SCHEDULED"))); } catch (Exception e) { /* default */ }
        m.setHomeScore(getIntField(parts, 7, 0));
        m.setAwayScore(getIntField(parts, 8, 0));
        m.setAttendance(getIntField(parts, 9, 0));
        m.setTicketPriceStandard(getDoubleField(parts, 10, 0.0));
        m.setTicketPriceVip(getDoubleField(parts, 11, 0.0));
        m.setCompetition(getField(parts, 12, null));
        m.setRound(getIntField(parts, 13, 0));
        m.setReferee(getField(parts, 14, null));
        return m;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static Match fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
