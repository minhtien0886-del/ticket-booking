package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.service.*;
import com.club.security.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Secure controller for staff management and personnel operations.
 *
 * <h3>Permission Matrix</h3>
 * <table border="1">
 *   <tr><th>Method</th>               <th>Required Permission</th>      <th>Roles Allowed</th></tr>
 *   <tr><td>updatePlayerFitness</td>  <td>MANAGE_PLAYER_FITNESS</td>  <td>HLV_TRUONG, GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>updatePlayerSalary</td>    <td>MODIFY_SALARY</td>          <td>GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>updateStaffSalary</td>    <td>MODIFY_SALARY</td>          <td>GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>getPersonnelReport</td>   <td>VIEW_FINANCIAL_REPORTS</td>  <td>GIAM_DOC_NHAN_SU, GIAM_DOC_TAI_CHINH, ADMIN</td></tr>
 *   <tr><td>getAllPlayers</td>        <td>VIEW_MATCHES</td>            <td>ALL</td></tr>
 *   <tr><td>getAllStaff</td>           <td>MANAGE_HUMAN_RESOURCE</td>   <td>GIAM_DOC_NHAN_SU, GIAM_DOC_TAI_CHINH, ADMIN</td></tr>
 *   <tr><td>getHealthyPlayers</td>     <td>VIEW_MATCHES</td>            <td>ALL</td></tr>
 *   <tr><td>hirePlayer</td>            <td>MANAGE_HUMAN_RESOURCE</td>   <td>GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>hireStaff</td>             <td>MANAGE_HUMAN_RESOURCE</td>   <td>GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>terminatePlayer</td>       <td>MANAGE_HUMAN_RESOURCE</td>   <td>GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>terminateStaff</td>         <td>MANAGE_HUMAN_RESOURCE</td>   <td>GIAM_DOC_NHAN_SU, ADMIN</td></tr>
 *   <tr><td>getTotalPayroll</td>       <td>VIEW_FINANCIAL_REPORTS</td>  <td>GIAM_DOC_NHAN_SU, GIAM_DOC_TAI_CHINH, ADMIN</td></tr>
 * </table>
 *
 * <h3>Role Restrictions</h3>
 * <ul>
 *   <li>HLV_TRUONG (coach): Can only mutate player fitness — cannot touch salaries or financial data.</li>
 *   <li>TRONG_TAI (commentator): Has no access to this controller — redirected to match stats only.</li>
 *   <li>GIAM_DOC_TAI_CHINH (finance): Can view reports and payroll but CANNOT modify salaries.</li>
 *   <li>FAN: Blocked from all operations by requireStaff() implicit in every method.</li>
 * </ul>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class StaffController {

    private final HumanResourceService hrService;
    private final PlayerRepository playerRepo;
    private final StaffRepository staffRepo;
    private final SecurityContext securityContext;

    public StaffController(HumanResourceService hrService,
                          PlayerRepository playerRepo,
                          StaffRepository staffRepo) {
        this.hrService = hrService;
        this.playerRepo = playerRepo;
        this.staffRepo = staffRepo;
        this.securityContext = SecurityContext.getInstance();
    }

    /**
     * Updates a player's fitness level.
     * <p>Required: MANAGE_PLAYER_FITNESS — allowed: HLV_TRUONG, GIAM_DOC_NHAN_SU, ADMIN</p>
     */
    public void updatePlayerFitness(String playerId, int newFitness) throws IOException {
        securityContext.requirePermission(Permission.MANAGE_PLAYER_FITNESS);
        hrService.updatePlayerFitness(playerId, newFitness);
    }

    /**
     * Updates a player's salary.
     * <p>Required: MODIFY_SALARY — allowed: GIAM_DOC_NHAN_SU, ADMIN (not GIAM_DOC_TAI_CHINH)</p>
     */
    public void updatePlayerSalary(String playerId, double newSalary) throws IOException {
        securityContext.requirePermission(Permission.MODIFY_SALARY);
        Player p = playerRepo.findById(playerId);
        if (p == null) throw new EntityNotFoundException("Player", playerId);
        p.setSalary(newSalary);
        playerRepo.save(p);
    }

    /**
     * Updates a staff member's salary.
     * <p>Required: MODIFY_SALARY — allowed: GIAM_DOC_NHAN_SU, ADMIN (not GIAM_DOC_TAI_CHINH)</p>
     */
    public void updateStaffSalary(String staffId, double newSalary) throws IOException {
        securityContext.requirePermission(Permission.MODIFY_SALARY);
        Staff s = staffRepo.findById(staffId);
        if (s == null) throw new EntityNotFoundException("Staff", staffId);
        s.setSalary(newSalary);
        staffRepo.save(s);
    }

    /**
     * Generates the full personnel report (players + staff + payroll).
     * <p>Required: VIEW_FINANCIAL_REPORTS — allowed: GIAM_DOC_NHAN_SU, GIAM_DOC_TAI_CHINH, ADMIN</p>
     */
    public HumanResourceService.PersonnelReport getPersonnelReport() {
        securityContext.requirePermission(Permission.VIEW_FINANCIAL_REPORTS);
        return hrService.generatePersonnelReport();
    }

    /**
     * Returns all players in the squad.
     * <p>Required: VIEW_MATCHES — allowed: ALL ROLES</p>
     */
    public List<Player> getAllPlayers() {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return hrService.getAllPlayers();
    }

    /**
     * Returns all staff members.
     * <p>Required: MANAGE_HUMAN_RESOURCE — allowed: GIAM_DOC_NHAN_SU, GIAM_DOC_TAI_CHINH, ADMIN</p>
     */
    public List<Staff> getAllStaff() {
        securityContext.requirePermission(Permission.MANAGE_HUMAN_RESOURCE);
        return hrService.getAllStaff();
    }

    /**
     * Returns all healthy (non-injured) players.
     * <p>Required: VIEW_MATCHES — allowed: ALL ROLES</p>
     */
    public List<Player> getHealthyPlayers() {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return hrService.getHealthyPlayers();
    }

    /**
     * Hires a new player.
     * <p>Required: MANAGE_HUMAN_RESOURCE — allowed: GIAM_DOC_NHAN_SU, ADMIN</p>
     */
    public void hirePlayer(Player player) throws IOException {
        securityContext.requirePermission(Permission.MANAGE_HUMAN_RESOURCE);
        hrService.hirePlayer(player);
    }

    /**
     * Hires a new staff member.
     * <p>Required: MANAGE_HUMAN_RESOURCE — allowed: GIAM_DOC_NHAN_SU, ADMIN</p>
     */
    public void hireStaff(Staff staff) throws IOException {
        securityContext.requirePermission(Permission.MANAGE_HUMAN_RESOURCE);
        hrService.hireStaff(staff);
    }

    /**
     * Terminates a player.
     * <p>Required: MANAGE_HUMAN_RESOURCE — allowed: GIAM_DOC_NHAN_SU, ADMIN</p>
     */
    public void terminatePlayer(String playerId) throws IOException {
        securityContext.requirePermission(Permission.MANAGE_HUMAN_RESOURCE);
        hrService.terminatePlayer(playerId);
    }

    /**
     * Terminates a staff member.
     * <p>Required: MANAGE_HUMAN_RESOURCE — allowed: GIAM_DOC_NHAN_SU, ADMIN</p>
     */
    public void terminateStaff(String staffId) throws IOException {
        securityContext.requirePermission(Permission.MANAGE_HUMAN_RESOURCE);
        hrService.terminateStaff(staffId);
    }

    /**
     * Returns total monthly payroll (players + staff).
     * <p>Required: VIEW_FINANCIAL_REPORTS — allowed: GIAM_DOC_NHAN_SU, GIAM_DOC_TAI_CHINH, ADMIN</p>
     */
    public double getTotalPayroll() {
        securityContext.requirePermission(Permission.VIEW_FINANCIAL_REPORTS);
        return hrService.getTotalPayroll();
    }

    @Override
    public String toString() {
        return "StaffController{service=" + hrService + "}";
    }
}
