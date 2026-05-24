package com.club.service;

import com.club.model.*;
import com.club.repository.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Inventory management service for merchandise products.
 * Handles stock tracking, variant management, and low-stock alerts.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class InventoryService {

    private final MerchandiseRepository merchandiseRepo;

    public InventoryService(MerchandiseRepository merchandiseRepo) {
        this.merchandiseRepo = merchandiseRepo;
    }

    /**
     * Retrieves all active merchandise items.
     */
    public List<Merchandise> getAllActiveMerchandise() {
        return merchandiseRepo.findActive();
    }

    /**
     * Retrieves merchandise by category.
     */
    public List<Merchandise> getByCategory(ProductCategory category) {
        return merchandiseRepo.findByCategory(category);
    }

    /**
     * Searches merchandise by name keyword.
     */
    public List<Merchandise> searchByName(String keyword) {
        return merchandiseRepo.searchByName(keyword);
    }

    /**
     * Retrieves in-stock items only.
     */
    public List<Merchandise> getInStockItems() {
        return merchandiseRepo.findInStock();
    }

    /**
     * Adds a new merchandise item.
     */
    public void addMerchandise(Merchandise item) throws IOException {
        merchandiseRepo.save(item);
    }

    /**
     * Updates stock quantity for a product.
     */
    public void updateStock(String productId, int newQuantity) throws IOException {
        Merchandise item = merchandiseRepo.findById(productId);
        if (item == null) {
            throw new EntityNotFoundException("Merchandise", productId);
        }
        merchandiseRepo.updateStock(productId, newQuantity);
    }

    /**
     * Reserves stock for a cart checkout.
     * Validates stock availability before reserving.
     *
     * @param productId the product ID
     * @param quantity the quantity to reserve
     * @throws OutOfStockException if insufficient stock
     * @throws EntityNotFoundException if product not found
     */
    public void reserveStock(String productId, int quantity) throws IOException {
        Merchandise item = merchandiseRepo.findById(productId);
        if (item == null) {
            throw new EntityNotFoundException("Merchandise", productId);
        }
        if (quantity > item.getStockQuantity()) {
            throw new OutOfStockException(productId, item.getName(), quantity, item.getStockQuantity());
        }
        merchandiseRepo.decrementStock(productId, quantity);
    }

    /**
     * Returns reserved stock (rollback during failed checkout).
     */
    public void returnStock(String productId, int quantity) throws IOException {
        Merchandise item = merchandiseRepo.findById(productId);
        if (item == null) {
            throw new EntityNotFoundException("Merchandise", productId);
        }
        merchandiseRepo.updateStock(productId, item.getStockQuantity() + quantity);
    }

    /**
     * Finds all items below the stock threshold.
     */
    public List<Merchandise> getLowStockItems(int threshold) {
        List<Merchandise> lowStock = new ArrayList<>();
        for (Merchandise item : merchandiseRepo.findAll()) {
            if (item.isActive() && item.getStockQuantity() < threshold) {
                lowStock.add(item);
            }
        }
        lowStock.sort(Comparator.comparingInt(Merchandise::getStockQuantity));
        return lowStock;
    }

    /**
     * Returns inventory statistics.
     */
    public InventoryStats getInventoryStats() {
        int totalProducts = 0;
        int activeProducts = 0;
        int inStockProducts = 0;
        int lowStockCount = 0;
        double totalValue = 0;

        for (Merchandise item : merchandiseRepo.findAll()) {
            totalProducts++;
            if (item.isActive()) activeProducts++;
            if (item.isInStock()) inStockProducts++;
            if (item.getStockQuantity() < 10) lowStockCount++;
            totalValue += item.getStockQuantity() * item.getBasePrice();
        }

        return new InventoryStats(totalProducts, activeProducts, inStockProducts,
            lowStockCount, totalValue);
    }

    /**
     * Inner class for inventory statistics.
     */
    public static class InventoryStats {
        public final int totalProducts;
        public final int activeProducts;
        public final int inStockProducts;
        public final int lowStockCount;
        public final double totalValue;

        public InventoryStats(int totalProducts, int activeProducts, int inStockProducts,
                            int lowStockCount, double totalValue) {
            this.totalProducts = totalProducts;
            this.activeProducts = activeProducts;
            this.inStockProducts = inStockProducts;
            this.lowStockCount = lowStockCount;
            this.totalValue = totalValue;
        }
    }

    @Override
    public String toString() {
        return "InventoryService{activeItems=" + merchandiseRepo.findActive().size() + "}";
    }
}
