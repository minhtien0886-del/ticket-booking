package com.club.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enumeration of all security roles within the FCM-ERP system.
 * Each role is mapped to a strict, immutable set of permissions following
 * the principle of least privilege. Roles represent job functions within
 * the football club organization.
 *
 * <p>The role hierarchy from highest to lowest privilege:</p>
 * <ol>
 *   <li>ADMIN — Full system access</li>
 *   <li>GIAM_DOC_TAI_CHINH — Financial director with HR read access</li>
 *   <li>GIAM_DOC_NHAN_SU — Human resources director</li>
 *   <li>HLV_TRUONG — Head coach with stats update capability</li>
 *   <li>TRONG_TAI — Commentator with league management access</li>
 *   <li>QUAN_LY_QUAY — Ticket counter manager</li>
 *   <li>FAN — End-user with limited view and purchase permissions</li>
 * </ol>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum Role {

    /**
     * Head Coach — manages team training, selects lineups, updates live match stats.
     * Permissions: UPDATE_LIVE_STATS, VIEW_SEAT_MAP, VIEW_MATCHES, MANAGE_LINEUP, MANAGE_PLAYER_FITNESS
     */
    HLV_TRUONG(
        Permission.UPDATE_LIVE_STATS,
        Permission.VIEW_SEAT_MAP,
        Permission.VIEW_MATCHES,
        Permission.MANAGE_LINEUP,
        Permission.MANAGE_PLAYER_FITNESS
    ),

    /**
     * Human Resources Director — manages all personnel, salary modifications.
     * Permissions: MANAGE_HUMAN_RESOURCE, MODIFY_SALARY, VIEW_SEAT_MAP, VIEW_MATCHES,
     *              VIEW_AUDIT_LOG, MANAGE_PLAYER_FITNESS
     */
    GIAM_DOC_NHAN_SU(
        Permission.MANAGE_HUMAN_RESOURCE,
        Permission.MODIFY_SALARY,
        Permission.VIEW_SEAT_MAP,
        Permission.VIEW_MATCHES,
        Permission.VIEW_AUDIT_LOG,
        Permission.MANAGE_PLAYER_FITNESS
    ),

    /**
     * Commentator / Analyst — provides match commentary and manages league data.
     * Permissions: UPDATE_LIVE_STATS, VIEW_SEAT_MAP, MANAGE_LEAGUE, VIEW_MATCHES
     */
    TRONG_TAI(
        Permission.UPDATE_LIVE_STATS,
        Permission.VIEW_SEAT_MAP,
        Permission.MANAGE_LEAGUE,
        Permission.VIEW_MATCHES
    ),

    /**
     * Ticket Counter Manager — processes ticket sales and manages stadium layout.
     * Permissions: PROCESS_TICKET, VIEW_SEAT_MAP, VIEW_MATCHES, PURCHASE_TICKET, MANAGE_CART
     */
    QUAN_LY_QUAY(
        Permission.PROCESS_TICKET,
        Permission.VIEW_SEAT_MAP,
        Permission.VIEW_MATCHES,
        Permission.PURCHASE_TICKET,
        Permission.MANAGE_CART
    ),

    /**
     * Financial Director — oversees all financial operations and reports.
     * Permissions: VIEW_FINANCIAL_REPORTS, PROCESS_PAYROLL, VIEW_MATCHES,
     *              MANAGE_MERCHANDISE, MANAGE_HUMAN_RESOURCE
     */
    GIAM_DOC_TAI_CHINH(
        Permission.VIEW_FINANCIAL_REPORTS,
        Permission.PROCESS_PAYROLL,
        Permission.VIEW_MATCHES,
        Permission.MANAGE_MERCHANDISE,
        Permission.MANAGE_HUMAN_RESOURCE
    ),

    /**
     * System Administrator — full unrestricted access to all system resources.
     * Permissions: ALL (every permission in the system)
     */
    ADMIN(Permission.allPermissions().toArray(new Permission[0])),

    /**
     * Fan / End User — limited access to view seats, matches, and make purchases.
     * Permissions: VIEW_SEAT_MAP, VIEW_MATCHES, PURCHASE_TICKET, MANAGE_CART,
     *               VIEW_LOYALTY, REGISTER_FAN
     */
    FAN(
        Permission.VIEW_SEAT_MAP,
        Permission.VIEW_MATCHES,
        Permission.PURCHASE_TICKET,
        Permission.MANAGE_CART,
        Permission.VIEW_LOYALTY,
        Permission.REGISTER_FAN
    );

    private final Set<Permission> permissions;

    Role(Permission... permissions) {
        this.permissions = Collections.unmodifiableSet(
            EnumSet.copyOf(Arrays.asList(permissions))
        );
    }

    /**
     * Returns the immutable set of permissions granted to this role.
     *
     * @return an unmodifiable Set of Permission enums
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Checks whether this role has the specified permission.
     *
     * @param permission the permission to check
     * @return true if the role has the permission, false otherwise
     */
    public boolean hasPermission(Permission permission) {
        return this.permissions.contains(permission);
    }

    /**
     * Checks whether this role has all the specified permissions.
     *
     * @param perms the permissions to check
     * @return true if the role has all permissions, false otherwise
     */
    public boolean hasAllPermissions(Permission... perms) {
        for (Permission p : perms) {
            if (!this.permissions.contains(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if this role is a staff role (non-fan).
     *
     * @return true if the role is not FAN
     */
    public boolean isStaffRole() {
        return this != FAN;
    }

    /**
     * Returns a human-readable description of this role.
     *
     * @return role description string
     */
    public String getDescription() {
        switch (this) {
            case HLV_TRUONG:     return "Head Coach (HLV Truong)";
            case GIAM_DOC_NHAN_SU: return "HR Director (Giam Doc Nhan Su)";
            case TRONG_TAI:      return "Commentator (Trong Tai)";
            case QUAN_LY_QUAY:   return "Ticket Manager (Quan Ly Quay)";
            case GIAM_DOC_TAI_CHINH: return "Financial Director (Giam Doc Tai Chinh)";
            case ADMIN:          return "System Administrator";
            case FAN:            return "Football Fan (Khach Hang)";
            default:             return this.name();
        }
    }
}
