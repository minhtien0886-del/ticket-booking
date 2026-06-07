package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.security.*;
import com.club.service.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Controller for merchandise shop operations. Acts as the exclusive intermediary
 * between the VIEW layer (SecureMenu) and the InventoryService/Model layer.
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class MerchandiseController {

    private final InventoryService inventoryService;
    private final MerchandiseRepository merchandiseRepo;
    private final SecurityContext securityContext;

    public MerchandiseController(InventoryService inventoryService,
                                  MerchandiseRepository merchandiseRepo) {
        this.inventoryService = inventoryService;
        this.merchandiseRepo = merchandiseRepo;
        this.securityContext = SecurityContext.getInstance();
    }

    /**
     * Returns all in-stock merchandise items.
     *
     * <p>Required: VIEW_SEAT_MAP or PURCHASE_TICKET (any authenticated user).</p>
     */
    public List<Merchandise> getInStockItems() {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        return inventoryService.getInStockItems();
    }

    /**
     * Returns all active merchandise items.
     */
    public List<Merchandise> getActiveItems() {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        return merchandiseRepo.findActive();
    }

    /**
     * Finds a merchandise product by ID.
     *
     * @param productId the product ID
     * @return the product or null
     */
    public Merchandise getProduct(String productId) {
        return merchandiseRepo.findById(productId);
    }

    /**
     * Returns all products in a given category.
     *
     * @param category the product category
     * @return matching products
     */
    public List<Merchandise> getByCategory(ProductCategory category) {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        return merchandiseRepo.findByCategory(category);
    }

    /**
     * Searches merchandise by name keyword.
     *
     * @param keyword case-insensitive keyword
     * @return matching products
     */
    public List<Merchandise> searchByName(String keyword) {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        return merchandiseRepo.searchByName(keyword);
    }
}
