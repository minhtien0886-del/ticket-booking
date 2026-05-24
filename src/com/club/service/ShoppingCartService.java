package com.club.service;

import com.club.model.*;
import com.club.repository.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Shopping cart service for managing fan merchandise purchases.
 * Provides add/remove operations, stock validation, and checkout processing.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class ShoppingCartService {

    private final CartItemRepository cartItemRepo;
    private final MerchandiseRepository merchandiseRepo;
    private final FanRepository fanRepo;
    private final FinanceService financeService;
    private final InventoryService inventoryService;

    public ShoppingCartService(CartItemRepository cartItemRepo,
                               MerchandiseRepository merchandiseRepo,
                               FanRepository fanRepo,
                               FinanceService financeService,
                               InventoryService inventoryService) {
        this.cartItemRepo = cartItemRepo;
        this.merchandiseRepo = merchandiseRepo;
        this.fanRepo = fanRepo;
        this.financeService = financeService;
        this.inventoryService = inventoryService;
    }

    /**
     * Adds an item to the fan's shopping cart.
     *
     * @param fanId the fan's ID
     * @param productId the product to add
     * @param quantity number of units
     * @param size optional size variant
     * @param color optional color variant
     * @return the created CartItem
     * @throws OutOfStockException if product is out of stock
     * @throws EntityNotFoundException if product not found
     */
    public CartItem addToCart(String fanId, String productId, int quantity,
                               String size, String color) throws IOException {
        Merchandise product = merchandiseRepo.findById(productId);
        if (product == null) {
            throw new EntityNotFoundException("Merchandise", productId);
        }
        if (!product.isInStock()) {
            throw new OutOfStockException(productId, product.getName(), quantity, product.getStockQuantity());
        }
        if (quantity <= 0 || quantity > 10) {
            throw new ValidationException("Quantity must be between 1 and 10: " + quantity);
        }

        CartItem item = new CartItem();
        item.setCartItemId(UUID.randomUUID().toString());
        item.setFanId(fanId);
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setQuantity(quantity);
        item.setSize(size);
        item.setColor(color);
        item.setUnitPrice(product.getBasePrice());
        item.setAddedAt(java.time.LocalDateTime.now().toString());

        cartItemRepo.save(item);
        return item;
    }

    /**
     * Updates the quantity of a cart item.
     */
    public void updateQuantity(String cartItemId, int newQuantity) throws IOException {
        CartItem item = cartItemRepo.findById(cartItemId);
        if (item == null) {
            throw new EntityNotFoundException("CartItem", cartItemId);
        }
        if (newQuantity <= 0) {
            removeFromCart(cartItemId);
            return;
        }
        item.setQuantity(newQuantity);
        cartItemRepo.save(item);
    }

    /**
     * Removes an item from the cart.
     */
    public void removeFromCart(String cartItemId) throws IOException {
        cartItemRepo.deleteById(cartItemId);
    }

    /**
     * Returns all items in the fan's cart.
     */
    public List<CartItem> getCartItems(String fanId) {
        return cartItemRepo.findByFanId(fanId);
    }

    /**
     * Returns the total value of the cart.
     */
    public double getCartTotal(String fanId) {
        return cartItemRepo.getCartTotal(fanId);
    }

    /**
     * Clears all items from the fan's cart.
     */
    public void clearCart(String fanId) throws IOException {
        cartItemRepo.clearByFanId(fanId);
    }

    /**
     * Processes checkout for the fan's cart.
     * Performs atomic transaction: validates balance, reserves stock, records transaction.
     *
     * @param fanId the fan's ID
     * @param processedBy the username of the processing staff
     * @return the transaction record
     * @throws InsufficientBalanceException if fan has insufficient balance
     * @throws OutOfStockException if any item is out of stock
     */
    public Transaction checkout(String fanId, String processedBy) throws IOException {
        List<CartItem> items = getCartItems(fanId);
        if (items.isEmpty()) {
            throw new ValidationException("Cart is empty");
        }

        Fan fan = fanRepo.findById(fanId);
        if (fan == null) {
            throw new EntityNotFoundException("Fan", fanId);
        }

        double total = getCartTotal(fanId);
        double discount = fan.getTicketDiscountRate();
        double discountedTotal = total * (1.0 - discount);

        if (fan.getAccountBalance() < discountedTotal) {
            throw new InsufficientBalanceException(fanId, fan.getAccountBalance(), discountedTotal);
        }

        for (CartItem item : items) {
            inventoryService.reserveStock(item.getProductId(), item.getQuantity());
        }

        try {
            fan.setAccountBalance(fan.getAccountBalance() - discountedTotal);
            fan.recordPurchase(discountedTotal);
            fanRepo.save(fan);

            String cartId = UUID.randomUUID().toString();
            Transaction t = financeService.recordMerchandiseSale(fanId, discountedTotal, cartId, processedBy);

            fan.addLoyaltyPoints((int) (discountedTotal / 100));

            clearCart(fanId);

            return t;
        } catch (Exception e) {
            for (CartItem item : items) {
                try {
                    inventoryService.returnStock(item.getProductId(), item.getQuantity());
                } catch (IOException ex) {
                    System.err.println("Failed to return stock during rollback: " + ex.getMessage());
                }
            }
            throw e;
        }
    }

    @Override
    public String toString() {
        return "ShoppingCartService{items=" + cartItemRepo.count() + "}";
    }
}
