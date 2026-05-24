package com.club.repository;

import com.club.model.*;
import com.club.model.Staff.SpecificRole;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link Staff} entities backed by staff.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class StaffRepository extends GenericCsvRepository<Staff> {

    private static final String HEADER = "id,name,email,specificRole,salary,department,startDate,endDate,active,qualifications,reportsTo";

    public StaffRepository(Path dataDir) {
        super(
            dataDir.resolve("staff.csv"),
            "id",
            Staff::getId,
            Staff::fromCsv,
            Staff::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<Staff> findByDepartment(String department) {
        return findAll(s -> department.equalsIgnoreCase(s.getDepartment()));
    }

    public List<Staff> findActiveStaff() {
        return findAll(Staff::isActive);
    }

    public List<Staff> findByRole(SpecificRole role) {
        return findAll(s -> s.getSpecificRole() == role);
    }

    public List<Staff> findBySalaryRange(double min, double max) {
        return findAll(s -> s.getSalary() >= min && s.getSalary() <= max);
    }

    public double getTotalSalaryBill() {
        return findAll().stream().mapToDouble(Staff::getSalary).sum();
    }

    @Override
    protected Staff parse(String csvLine) {
        return Staff.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Staff entity) {
        return entity.toCsv();
    }
}
