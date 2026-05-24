package com.club.model;

import java.util.Objects;

/**
 * Concrete {@link Person} subclass representing a football fan registered in the club's
 * loyalty program. Fans are end-users who can purchase tickets, merchandise, and manage
 * their account balance within the FCM-ERP system.
 *
 * <p>Each fan has a loyalty tier (Bronze/Silver/Gold/Platinum/Diamond) that determines
 * their discount rates, priority access, and reward redemption capabilities. Tiers are
 * calculated dynamically based on accumulated loyalty points.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see Person
 * @see LoyaltyTier
 */
public class Fan extends Person {

    private static final long serialVersionUID = 1L;

    /** Account balance for purchasing tickets and merchandise. */
    private double accountBalance;

    /** Accumulated loyalty points from purchases. */
    private int loyaltyPoints;

    /** Current loyalty tier derived from loyaltyPoints. */
    private LoyaltyTier tier;

    /** Registered mobile phone number. */
    private String phoneNumber;

    /** Home address for merchandise delivery. */
    private String address;

    /** Date of registration in the loyalty program (ISO format). */
    private String registeredDate;

    /** Preferred seat sector for ticket purchases. */
    private String preferredSector;

    /** Number of total tickets purchased. */
    private int totalTicketsPurchased;

    /** Total monetary value of all purchases made. */
    private double totalSpend;

    /** Whether the fan has opted into marketing communications. */
    private boolean marketingOptIn;

    /**
     * Default constructor.
     */
    public Fan() {
        super();
        this.accountBalance = 0.0;
        this.loyaltyPoints = 0;
        this.tier = LoyaltyTier.BRONZE;
        this.totalTicketsPurchased = 0;
        this.totalSpend = 0.0;
        this.marketingOptIn = false;
    }

    /**
     * Full constructor.
     */
    public Fan(String id, String name, String email, double accountBalance,
              int loyaltyPoints, String phoneNumber) {
        super(id, name, email);
        this.accountBalance = accountBalance;
        this.loyaltyPoints = loyaltyPoints;
        this.tier = LoyaltyTier.fromPoints(loyaltyPoints);
        this.phoneNumber = phoneNumber;
        this.totalTicketsPurchased = 0;
        this.totalSpend = 0.0;
        this.marketingOptIn = false;
    }

    /**
     * Simplified constructor for new fan registration.
     */
    public Fan(String name, String email, double initialBalance) {
        super(name, email);
        this.accountBalance = initialBalance;
        this.loyaltyPoints = 0;
        this.tier = LoyaltyTier.BRONZE;
        this.totalTicketsPurchased = 0;
        this.totalSpend = 0.0;
        this.marketingOptIn = false;
    }

    @Override
    public String getPersonType() {
        return "FAN";
    }

    // ============ GETTERS AND SETTERS ============

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double accountBalance) {
        if (accountBalance < 0) {
            throw new IllegalArgumentException("Account balance cannot be negative");
        }
        this.accountBalance = accountBalance;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = Math.max(0, loyaltyPoints);
        this.tier = LoyaltyTier.fromPoints(this.loyaltyPoints);
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public void setTier(LoyaltyTier tier) {
        this.tier = tier != null ? tier : LoyaltyTier.BRONZE;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    public String getPreferredSector() {
        return preferredSector;
    }

    public void setPreferredSector(String preferredSector) {
        this.preferredSector = preferredSector;
    }

    public int getTotalTicketsPurchased() {
        return totalTicketsPurchased;
    }

    public void setTotalTicketsPurchased(int totalTicketsPurchased) {
        this.totalTicketsPurchased = Math.max(0, totalTicketsPurchased);
    }

    public double getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(double totalSpend) {
        this.totalSpend = Math.max(0, totalSpend);
    }

    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    // ============ BUSINESS METHODS ============

    /**
     * Adds loyalty points and recalculates the fan's tier.
     *
     * @param points the number of points to add
     */
    public void addLoyaltyPoints(int points) {
        if (points > 0) {
            this.loyaltyPoints += points;
            this.tier = LoyaltyTier.fromPoints(this.loyaltyPoints);
        }
    }

    /**
     * Redeems loyalty points for a discount.
     *
     * @param pointsToRedeem the number of points to redeem
     * @return the monetary discount value
     */
    public double redeemLoyaltyPoints(int pointsToRedeem) {
        if (pointsToRedeem > this.loyaltyPoints) {
            throw new IllegalArgumentException("Insufficient loyalty points");
        }
        this.loyaltyPoints -= pointsToRedeem;
        this.tier = LoyaltyTier.fromPoints(this.loyaltyPoints);
        return pointsToRedeem / 100.0;
    }

    /**
     * Adds funds to the fan's account balance.
     *
     * @param amount the amount to deposit
     */
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.accountBalance += amount;
    }

    /**
     * Deducts funds from the fan's account balance.
     *
     * @param amount the amount to withdraw
     * @return true if successful, false if insufficient balance
     */
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > this.accountBalance) {
            return false;
        }
        this.accountBalance -= amount;
        return true;
    }

    /**
     * Records a purchase and updates loyalty points accordingly.
     * For every 100 units spent, the fan earns 1 loyalty point.
     *
     * @param purchaseAmount the total amount of the purchase
     */
    public void recordPurchase(double purchaseAmount) {
        if (purchaseAmount <= 0) {
            throw new IllegalArgumentException("Purchase amount must be positive");
        }
        this.totalSpend += purchaseAmount;
        this.totalTicketsPurchased++;
        int pointsEarned = (int) (purchaseAmount / 100);
        addLoyaltyPoints(pointsEarned);
    }

    /**
     * Calculates the discount applicable to the next ticket purchase.
     *
     * @return the discount multiplier (e.g., 0.10 for 10% off)
     */
    public double getTicketDiscountRate() {
        return tier.getDiscountRate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Fan fan = (Fan) o;
        return Double.compare(fan.accountBalance, accountBalance) == 0 &&
               loyaltyPoints == fan.loyaltyPoints &&
               Objects.equals(tier, fan.tier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountBalance, loyaltyPoints, tier);
    }

    @Override
    public String toString() {
        return String.format(
            "Fan{id='%s', name='%s', email='%s', balance=%.2f, points=%d, tier=%s, totalSpend=%.2f}",
            id, name, email, accountBalance, loyaltyPoints, tier, totalSpend
        );
    }

    public String toCsv() {
        return String.join(",",
            safe(id),
            safe(name),
            safe(email),
            String.valueOf(accountBalance),
            String.valueOf(loyaltyPoints),
            tier != null ? tier.name() : "BRONZE",
            safe(phoneNumber),
            safe(address),
            safe(registeredDate),
            safe(preferredSector),
            String.valueOf(totalTicketsPurchased),
            String.valueOf(totalSpend),
            String.valueOf(marketingOptIn)
        );
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static Fan fromCsv(String csv) {
        String[] parts = parseCsvLine(csv);
        Fan f = new Fan();
        f.setId(parts[0]);
        f.setName(parts[1]);
        f.setEmail(parts[2]);
        try { f.setAccountBalance(Double.parseDouble(parts[3])); } catch (Exception e) { /* default 0 */ }
        try { f.setLoyaltyPoints(Integer.parseInt(parts[4])); } catch (Exception e) { /* default 0 */ }
        try { f.setTier(LoyaltyTier.valueOf(parts[5])); } catch (Exception e) { /* default bronze */ }
        f.setPhoneNumber(parts.length > 6 ? parts[6] : null);
        f.setAddress(parts.length > 7 ? parts[7] : null);
        f.setRegisteredDate(parts.length > 8 ? parts[8] : null);
        f.setPreferredSector(parts.length > 9 ? parts[9] : null);
        try { f.setTotalTicketsPurchased(Integer.parseInt(parts.length > 10 ? parts[10] : "0")); } catch (Exception e) { /* default 0 */ }
        try { f.setTotalSpend(Double.parseDouble(parts.length > 11 ? parts[11] : "0")); } catch (Exception e) { /* default 0 */ }
        try { f.setMarketingOptIn(Boolean.parseBoolean(parts.length > 12 ? parts[12] : "false")); } catch (Exception e) { /* default false */ }
        return f;
    }

    private static String[] parseCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return new String[13];
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
