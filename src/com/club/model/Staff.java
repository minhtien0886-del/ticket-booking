package com.club.model;

import java.util.Objects;

/**
 * Concrete {@link Person} subclass representing a staff member of the football club.
 * Staff members are club employees who are not players, such as coaches, medical
 * staff, administrative personnel, and support workers.
 *
 * <p>Each staff member has a specific role (e.g., ASSISTANT_COACH, PHYSIOTHERAPIST)
 * that determines their permissions and responsibilities within the club hierarchy.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see Person
 */
public class Staff extends Person {

    private static final long serialVersionUID = 1L;

    /**
     * Enumeration of specific staff roles within the club organization.
     */
    public enum SpecificRole {
        HEAD_COACH,
        ASSISTANT_COACH,
        GOALKEEPER_COACH,
        FITNESS_COACH,
        PHYSIOTHERAPIST,
        SPORTS_SCIENTIST,
        DOCTOR,
        TECHNICAL_DIRECTOR,
        DIRECTOR_OF_FOOTBALL,
        MEDIA_OFFICER,
        SECURITY_CHIEF,
        GROUNDSMAN,
        KIT_MANAGER,
        CLEANING_STAFF,
        CATERING_STAFF,
        ADMIN_MANAGER,
        ACCOUNTANT,
        HR_MANAGER,
        MARKETING_MANAGER,
        IT_ADMINISTRATOR
    }

    /** The specific departmental role of this staff member. */
    private SpecificRole specificRole;

    /** Monthly salary in currency units. */
    private double salary;

    /** Department or section the staff member belongs to. */
    private String department;

    /** Employment start date (ISO format YYYY-MM-DD). */
    private String startDate;

    /** Contract end date (ISO format YYYY-MM-DD). */
    private String endDate;

    /** Employment status of the staff member. */
    private boolean active;

    /** Qualification level or certification codes. */
    private String qualifications;

    /** Reports to (ID of the supervisor). */
    private String reportsTo;

    /**
     * Default constructor.
     */
    public Staff() {
        super();
        this.active = true;
        this.salary = 0.0;
    }

    /**
     * Full constructor.
     */
    public Staff(String id, String name, String email, SpecificRole specificRole,
                 double salary, String department) {
        super(id, name, email);
        this.specificRole = specificRole;
        setSalary(salary);
        this.department = department;
        this.active = true;
    }

    /**
     * Simplified constructor.
     */
    public Staff(String name, String email, SpecificRole specificRole, double salary) {
        super(name, email);
        this.specificRole = specificRole;
        setSalary(salary);
        this.department = determineDepartment(specificRole);
        this.active = true;
    }

    @Override
    public String getPersonType() {
        return "STAFF";
    }

    private static String determineDepartment(SpecificRole role) {
        if (role == null) return "UNKNOWN";
        switch (role) {
            case HEAD_COACH: case ASSISTANT_COACH: case GOALKEEPER_COACH:
            case FITNESS_COACH: case TECHNICAL_DIRECTOR: case DIRECTOR_OF_FOOTBALL:
                return "COACHING";
            case PHYSIOTHERAPIST: case SPORTS_SCIENTIST: case DOCTOR:
                return "MEDICAL";
            case MEDIA_OFFICER: case MARKETING_MANAGER:
                return "MARKETING";
            case SECURITY_CHIEF: case GROUNDSMAN: case KIT_MANAGER:
            case CLEANING_STAFF: case CATERING_STAFF:
                return "OPERATIONS";
            case ADMIN_MANAGER: case HR_MANAGER: case ACCOUNTANT: case IT_ADMINISTRATOR:
                return "ADMINISTRATION";
            default:
                return "GENERAL";
        }
    }

    // ============ GETTERS AND SETTERS ============

    public SpecificRole getSpecificRole() {
        return specificRole;
    }

    public void setSpecificRole(SpecificRole specificRole) {
        if (specificRole == null) {
            throw new IllegalArgumentException("Specific role cannot be null");
        }
        this.specificRole = specificRole;
        if (this.department == null) {
            this.department = determineDepartment(specificRole);
        }
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative");
        }
        this.salary = salary;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getReportsTo() {
        return reportsTo;
    }

    public void setReportsTo(String reportsTo) {
        this.reportsTo = reportsTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Staff staff = (Staff) o;
        return active == staff.active &&
               Double.compare(staff.salary, salary) == 0 &&
               Objects.equals(specificRole, staff.specificRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), specificRole, salary, active);
    }

    @Override
    public String toString() {
        return String.format(
            "Staff{id='%s', name='%s', email='%s', role=%s, dept=%s, salary=%.2f, active=%s}",
            id, name, email, specificRole, department, salary, active
        );
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(id),
            safe(name),
            safe(email),
            specificRole != null ? specificRole.name() : "",
            String.valueOf(salary),
            safe(department),
            safe(startDate),
            safe(endDate),
            String.valueOf(active),
            safe(qualifications),
            safe(reportsTo)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static Staff fromCsvLine(String csv) {
        String[] parts = parseCsvLine(csv);
        if (parts.length < 3) return null;
        Staff s = new Staff();
        s.setId(parts[0]);
        s.setName(parts[1]);
        s.setEmail(parts[2]);
        try { s.setSpecificRole(SpecificRole.valueOf(getField(parts, 3, "ADMIN_MANAGER"))); } catch (Exception e) { /* default */ }
        s.setSalary(getDoubleField(parts, 4, 0.0));
        s.setDepartment(getField(parts, 5, null));
        s.setStartDate(getField(parts, 6, null));
        s.setEndDate(getField(parts, 7, null));
        s.setActive(getBooleanField(parts, 8, true));
        s.setQualifications(getField(parts, 9, null));
        s.setReportsTo(getField(parts, 10, null));
        return s;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static Staff fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
