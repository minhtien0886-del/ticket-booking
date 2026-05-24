package com.club.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enumeration of all granular permissions available within the FCM-ERP system.
 * Each permission represents a specific operational capability that can be
 * granted to a role. Permissions follow the principle of least privilege.
 *
 * <p>Permissions are immutable and form the atomic unit of access control.
 * They are grouped into logical categories: HR, Finance, Operations, and System.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum Permission {

    /** Permission to manage league tables, standings, and match scheduling. */
    MANAGE_LEAGUE,

    /** Permission to update real-time match statistics and live game data. */
    UPDATE_LIVE_STATS,

    /** Permission to modify employee salary records and compensation structures. */
    MODIFY_SALARY,

    /** Permission to process ticket bookings, cancellations, and refunds. */
    PROCESS_TICKET,

    /** Permission to view financial reports, balance sheets, and expense summaries. */
    VIEW_FINANCIAL_REPORTS,

    /** Permission to manage human resources: hiring, firing, and role assignment. */
    MANAGE_HUMAN_RESOURCE,

    /** Permission to execute the load-testing simulator engine. */
    RUN_SIMULATOR,

    /** Permission to view the stadium seat availability map. */
    VIEW_SEAT_MAP,

    /** Permission to register new fan accounts in the system. */
    REGISTER_FAN,

    /** Permission to manage merchandise inventory and product catalog. */
    MANAGE_MERCHANDISE,

    /** Permission to process payroll for staff and players. */
    PROCESS_PAYROLL,

    /** Permission to view match schedules and upcoming events. */
    VIEW_MATCHES,

    /** Permission to manage match lineups and team selections. */
    MANAGE_LINEUP,

    /** Permission to access system audit logs and security trails. */
    VIEW_AUDIT_LOG,

    /** Permission to purchase tickets and merchandise. */
    PURCHASE_TICKET,

    /** Permission to manage shopping cart and checkout. */
    MANAGE_CART,

    /** Permission to manage player fitness and injury records. */
    MANAGE_PLAYER_FITNESS,

    /** Permission to view loyalty program details and points. */
    VIEW_LOYALTY;

    /**
     * Returns an unmodifiable view of all permissions in the system.
     *
     * @return a set containing every permission enum value
     */
    public static Set<Permission> allPermissions() {
        return Collections.unmodifiableSet(EnumSet.allOf(Permission.class));
    }
}
