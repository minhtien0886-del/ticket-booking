package com.club.model;

/**
 * Enumeration of player positions on the football pitch.
 * Used for team selection, tactical planning, and player statistics.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum Position {

    GOALKEEPER("GK", "Goalkeeper — guards the goal", 1),
    CENTER_BACK("CB", "Center Back — central defensive role", 2),
    LEFT_BACK("LB", "Left Back — left defensive flank", 2),
    RIGHT_BACK("RB", "Right Back — right defensive flank", 2),
    DEFENSIVE_MIDFIELDER("DM", "Defensive Midfielder — shields defense", 3),
    CENTRAL_MIDFIELDER("CM", "Central Midfielder — controls midfield", 3),
    ATTACKING_MIDFIELDER("AM", "Attacking Midfielder — creative playmaker", 4),
    LEFT_WINGER("LW", "Left Winger — attacks left flank", 4),
    RIGHT_WINGER("RW", "Right Winger — attacks right flank", 4),
    STRIKER("ST", "Striker — primary goal scorer", 5),
    FORWARD("FW", "Forward — versatile attacking role", 5);

    private final String abbreviation;
    private final String description;
    private final int defaultLine;

    Position(String abbreviation, String description, int defaultLine) {
        this.abbreviation = abbreviation;
        this.description = description;
        this.defaultLine = defaultLine;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultLine() {
        return defaultLine;
    }

    public boolean isGoalkeeper() {
        return this == GOALKEEPER;
    }

    public boolean isDefender() {
        return this == CENTER_BACK || this == LEFT_BACK || this == RIGHT_BACK;
    }

    public boolean isMidfielder() {
        return this == DEFENSIVE_MIDFIELDER || this == CENTRAL_MIDFIELDER
            || this == ATTACKING_MIDFIELDER;
    }

    public boolean isForward() {
        return this == STRIKER || this == FORWARD || this == LEFT_WINGER || this == RIGHT_WINGER;
    }
}
