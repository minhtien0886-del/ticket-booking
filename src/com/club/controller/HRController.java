package com.club.controller;

import com.club.model.*;
import com.club.service.*;
import com.club.security.*;
import java.io.IOException;
import java.util.*;

/**
 * Controller for human resources reporting operations.
 * Acts as the exclusive intermediary between the VIEW layer and HumanResourceService.
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class HRController {

    private final HumanResourceService hrService;
    private final SecurityContext securityContext;

    public HRController(HumanResourceService hrService) {
        this.hrService = hrService;
        this.securityContext = SecurityContext.getInstance();
    }

    /**
     * Generates and returns the complete personnel report.
     *
     * <p>Required: VIEW_FINANCIAL_REPORTS permission.</p>
     *
     * @return the personnel report
     */
    public HumanResourceService.PersonnelReport generatePersonnelReport() {
        securityContext.requirePermission(Permission.VIEW_FINANCIAL_REPORTS);
        return hrService.generatePersonnelReport();
    }

    /**
     * Returns all players.
     *
     * <p>Required: VIEW_MATCHES permission.</p>
     */
    public java.util.List<Player> getAllPlayers() {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        return hrService.getAllPlayers();
    }

    /**
     * Returns all staff members.
     *
     * <p>Required: MANAGE_HUMAN_RESOURCE permission.</p>
     */
    public java.util.List<Staff> getAllStaff() {
        securityContext.requirePermission(Permission.MANAGE_HUMAN_RESOURCE);
        return hrService.getAllStaff();
    }
}
