package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.security.*;
import com.club.service.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Secure command interceptor for ticket booking operations.
 * Every public method enforces RBAC via SecurityContext before execution.
 *
 * <h3>Permission Matrix</h3>
 * <table border="1">
 *   <tr><th>Method</th>           <th>Required Permission</th> <th>Roles Allowed</th></tr>
 *   <tr><td>bookTickets</td>       <td>PURCHASE_TICKET</td>     <td>FAN, QUAN_LY_QUAY, ADMIN</td></tr>
 *   <tr><td>bookTicketsForFan</td>  <td>PROCESS_TICKET</td>     <td>QUAN_LY_QUAY, ADMIN</td></tr>
 *   <tr><td>cancelTicket</td>      <td>PROCESS_TICKET</td>     <td>QUAN_LY_QUAY, ADMIN</td></tr>
 *   <tr><td>viewAvailableMatches</td> <td>VIEW_MATCHES</td>       <td>ALL ROLES</td></tr>
 *   <tr><td>getMatchDetails</td>    <td>VIEW_MATCHES</td>       <td>ALL ROLES</td></tr>
 *   <tr><td>getSeatStatusCounts</td> <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 *   <tr><td>setLockMode</td>         <td>RUN_SIMULATOR</td>      <td>ADMIN, QUAN_LY_QUAY</td></tr>
 * </table>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class TicketController {

    private final TicketService ticketService;
    private final SecurityContext securityContext;
    private final MatchRepository matchRepo;
    private final SeatRepository seatRepo;

    public TicketController(TicketService ticketService,
                          MatchRepository matchRepo,
                          SeatRepository seatRepo) {
        this.ticketService = ticketService;
        this.securityContext = SecurityContext.getInstance();
        this.matchRepo = matchRepo;
        this.seatRepo = seatRepo;
    }

    /**
     * Books tickets for the currently authenticated fan.
     *
     * <p>Permission required: {@link Permission#PURCHASE_TICKET}</p>
     * <p>Allowed roles: FAN, QUAN_LY_QUAY, ADMIN</p>
     *
     * @param matchId  the target match
     * @param seatIds seats to book (max 4)
     * @return issued tickets
     */
    public List<Ticket> bookTickets(String matchId, List<String> seatIds) throws IOException {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        UserSession session = securityContext.getCurrentSession();
        String fanId = session.getPersonId();
        String processedBy = session.getUsername();
        return ticketService.bookTickets(fanId, matchId, seatIds, processedBy);
    }

    /**
     * Books tickets on behalf of a specific fan (staff-assisted booking).
     *
     * <p>Permission required: {@link Permission#PROCESS_TICKET}</p>
     * <p>Allowed roles: QUAN_LY_QUAY, ADMIN</p>
     *
     * @param fanId        fan's ID
     * @param matchId      target match
     * @param seatIds       seats to book
     * @param processedBy   staff username
     */
    public List<Ticket> bookTicketsForFan(String fanId, String matchId,
                                         List<String> seatIds, String processedBy) throws IOException {
        securityContext.requirePermission(Permission.PROCESS_TICKET);
        return ticketService.bookTickets(fanId, matchId, seatIds, processedBy);
    }

    /**
     * Cancels a confirmed ticket booking.
     *
     * <p>Permission required: {@link Permission#PROCESS_TICKET}</p>
     * <p>Allowed roles: QUAN_LY_QUAY, ADMIN</p>
     */
    public void cancelTicket(String ticketId) throws IOException {
        securityContext.requirePermission(Permission.PROCESS_TICKET);
        ticketService.cancelTicket(ticketId);
    }

    /**
     * Lists all upcoming matches available for ticket purchase.
     *
     * <p>Permission required: {@link Permission#VIEW_MATCHES}</p>
     * <p>Allowed roles: ALL (including FAN)</p>
     */
    public List<Match> viewAvailableMatches() {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return matchRepo.findUpcoming();
    }

    /**
     * Returns details for a specific match.
     *
     * <p>Permission required: {@link Permission#VIEW_MATCHES}</p>
     * <p>Allowed roles: ALL</p>
     */
    public Match getMatchDetails(String matchId) {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return matchRepo.findById(matchId);
    }

    /**
     * Returns seat availability counts by status.
     *
     * <p>Permission required: {@link Permission#VIEW_SEAT_MAP}</p>
     * <p>Allowed roles: ALL</p>
     */
    public Map<String, Long> getSeatStatusCounts() {
        securityContext.requirePermission(Permission.VIEW_SEAT_MAP);
        return seatRepo.getStatusCounts();
    }

    /**
     * Configures the active locking mode for concurrent booking.
     *
     * <p>Permission required: {@link Permission#RUN_SIMULATOR}</p>
     * <p>Allowed roles: ADMIN, QUAN_LY_QUAY</p>
     *
     * <p>ADMIN bypasses the permission check entirely (has ALL permissions).</p>
     *
     * @param mode the locking strategy
     */
    public void setLockMode(TicketService.LockMode mode) {
        UserSession session = securityContext.getCurrentSession();
        if (session.isAdmin()) {
            // ADMIN has all permissions — allow without further check.
            ticketService.setLockMode(mode);
        } else {
            // QUAN_LY_QUAY and others need explicit RUN_SIMULATOR permission.
            securityContext.requirePermission(Permission.RUN_SIMULATOR);
            ticketService.setLockMode(mode);
        }
    }

    @Override
    public String toString() {
        return "TicketController{service=" + ticketService + "}";
    }
}
