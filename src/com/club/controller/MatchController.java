package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.security.*;
import java.util.*;

/**
 * Controller for match-related operations. Acts as the exclusive intermediary
 * between the VIEW layer (SecureMenu) and the MatchRepository/Model layer.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Return match lists to VIEW for rendering</li>
 *   <li>Translate VIEW seat-input strings into concrete seat IDs</li>
 *   <li>Enforce RBAC via SecurityContext before any operation</li>
 * </ul>
 *
 * <p>No direct CSV I/O, no System.out/System.err, no business rule evaluation
 * beyond input translation and basic validation.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class MatchController {

    private final MatchRepository matchRepo;
    private final SeatRepository seatRepo;
    private final SecurityContext securityContext;

    public MatchController(MatchRepository matchRepo, SeatRepository seatRepo) {
        this.matchRepo = matchRepo;
        this.seatRepo = seatRepo;
        this.securityContext = SecurityContext.getInstance();
    }

    /**
     * Returns all upcoming matches.
     *
     * <p>Required: VIEW_MATCHES permission.</p>
     *
     * @return list of upcoming matches
     */
    public List<Match> getUpcomingMatches() {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return matchRepo.findUpcoming();
    }

    /**
     * Returns all completed matches.
     *
     * <p>Required: VIEW_MATCHES permission.</p>
     *
     * @return list of completed matches
     */
    public List<Match> getCompletedMatches() {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return matchRepo.findCompleted();
    }

    /**
     * Returns a match by its ID, or null if not found.
     *
     * <p>Required: VIEW_MATCHES permission.</p>
     *
     * @param matchId the match ID
     * @return the match or null
     */
    public Match getMatch(String matchId) {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return matchRepo.findById(matchId);
    }

    /**
     * Translates raw user-input tokens from the seat-selection UI into concrete
     * seat ID strings. Supports four formats:
     *
     * <ol>
     *   <li><b>Full ID</b>: {@code VIP-A-012} — passed through unchanged</li>
     *   <li><b>Sector-Row-Position</b>: {@code VIP-C-S12} — resolved via cache query</li>
     *   <li><b>Sector-Position</b>: {@code A-S01} — row resolved by walking sectors</li>
     *   <li><b>Position-only</b>: {@code S01} — resolved across entire stadium</li>
     * </ol>
     *
     * <p>This method is the only place where seat input parsing logic lives —
     * keeping it here ensures VIEW and service layers remain clean of UI concerns.</p>
     *
     * @param input  comma-separated raw user tokens
     * @param match  the target match (used only for global-position resolution)
     * @return list of concrete seat IDs
     */
    public List<String> resolveSeatTokens(String input, Match match) {
        List<String> resolved = new ArrayList<>();
        for (String token : input.split(",")) {
            String tok = token.trim();
            if (tok.isEmpty()) continue;

            // FORMAT 1: Full ID — SECTOR-ROW-NUMBER
            if (tok.matches("^[A-Z]+-[A-Z]-\\d{3,4}$")) {
                resolved.add(tok);
                continue;
            }

            // FORMAT 2: Sector-Row-Position — VIP-C-S12
            if (tok.matches("^[A-Z_]+-[A-Z]-S\\d{1,3}$")) {
                int dashS = tok.lastIndexOf("-S");
                String sectorAndRow = tok.substring(0, dashS);
                String posStr = tok.substring(dashS + 1);
                int firstDash = sectorAndRow.indexOf('-');
                String sector = sectorAndRow.substring(0, firstDash);
                int posIndex = Integer.parseInt(posStr.substring(1)) - 1;

                List<Seat> rowSeats = new ArrayList<>(seatRepo.findBySectorAndRow(sector, tok.substring(dashS - 1, dashS)));
                rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));

                List<Seat> allRowSeats = new ArrayList<>(seatRepo.findBySector(sector));
                final String rowLetter = tok.substring(dashS - 1, dashS);
                allRowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));
                List<Seat> targetRow = new ArrayList<>();
                for (Seat s : allRowSeats) {
                    if (s.getRowNum().equals(rowLetter)) targetRow.add(s);
                }
                targetRow.sort(Comparator.comparingInt(Seat::getSeatNumber));
                if (posIndex >= 0 && posIndex < targetRow.size()) {
                    resolved.add(targetRow.get(posIndex).getSeatId());
                }
                continue;
            }

            // FORMAT 3: Sector-Position — A-S01
            if (tok.matches("^[A-Z]+-S\\d{1,3}$")) {
                int dashS = tok.lastIndexOf("-S");
                String sector = tok.substring(0, dashS);
                int posIndex = Integer.parseInt(tok.substring(dashS + 2)) - 1;

                List<Seat> sectorSeats = new ArrayList<>(seatRepo.findBySector(sector));
                Map<String, List<Seat>> byRow = new LinkedHashMap<>();
                for (Seat s : sectorSeats) {
                    byRow.computeIfAbsent(s.getRowNum(), k -> new ArrayList<>()).add(s);
                }
                int globalIdx = 0;
                outer:
                for (Map.Entry<String, List<Seat>> entry : byRow.entrySet()) {
                    List<Seat> sorted = entry.getValue();
                    sorted.sort(Comparator.comparingInt(Seat::getSeatNumber));
                    for (Seat s : sorted) {
                        if (globalIdx == posIndex) {
                            resolved.add(s.getSeatId());
                            break outer;
                        }
                        globalIdx++;
                    }
                }
                continue;
            }

            // FORMAT 4: Position-only — S01 (global stadium scan)
            if (tok.matches("^S\\d{1,3}$")) {
                int posIndex = Integer.parseInt(tok.substring(1)) - 1;
                int globalIdx = 0;
                for (Seat seat : seatRepo.findAll()) {
                    if (globalIdx == posIndex) {
                        resolved.add(seat.getSeatId());
                        break;
                    }
                    globalIdx++;
                }
                continue;
            }

            // FALLBACK: Unknown format — pass through unchanged
            resolved.add(tok);
        }
        return resolved;
    }
}
