package com.club.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class representing a person entity within the football club system.
 * Serves as the root of the entity hierarchy for Players, Staff, and Fans.
 * Implements {@link Serializable} to support session caching and distributed caching.
 *
 * <p>All concrete person types inherit the core attributes: id, name, and email.
 * Subclasses must implement validation specific to their domain (e.g., position
 * for Players, specificRole for Staff).</p>
 *
 * <p>The ID is auto-generated as a UUID string when not provided, ensuring
 * universal uniqueness across distributed systems.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public abstract class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Auto-generated unique identifier using UUID. */
    protected String id;

    /** Full legal name of the person. */
    protected String name;

    /** Contact email address — must be unique within the system. */
    protected String email;

    /**
     * Default constructor that auto-generates a UUID for the person.
     */
    protected Person() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructor with name and email, auto-generating the ID.
     *
     * @param name  the person's full name
     * @param email the person's email address
     */
    protected Person(String name, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }

    /**
     * Full constructor with explicit ID.
     *
     * @param id    the unique identifier
     * @param name  the person's full name
     * @param email the person's email address
     */
    protected Person(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Person ID cannot be null or empty");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Person name cannot be null or empty");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    /**
     * Returns the type discriminator for polymorphic serialization.
     *
     * @return a string representing the concrete person type
     */
    public abstract String getPersonType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Person{id='%s', name='%s', email='%s', type=%s}",
            id, name, email, getPersonType());
    }
}
