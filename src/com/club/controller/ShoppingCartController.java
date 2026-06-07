package com.club.controller;

import com.club.model.*;
import com.club.service.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Controller for shopping cart and checkout operations.
 * Acts as the exclusive intermediary between the VIEW layer and ShoppingCartService.
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    /**
     * Returns the current user's cart items.
     *
     * @param fanId the fan's ID
     * @return list of cart items (empty list if cart is empty)
     */
    public List<CartItem> getCartItems(String fanId) {
        return shoppingCartService.getCartItems(fanId);
    }

    /**
     * Adds an item to the shopping cart.
     *
     * @param fanId   the fan's ID
     * @param productId the product ID
     * @param quantity  quantity to add
     * @param size      selected size (nullable)
     * @param color     selected color (nullable)
     * @return the resulting cart item
     * @throws EntityNotFoundException if the product does not exist
     */
    public CartItem addToCart(String fanId, String productId, int quantity,
                              String size, String color) throws IOException {
        return shoppingCartService.addToCart(fanId, productId, quantity, size, color);
    }

    /**
     * Returns the cart total for the given fan.
     *
     * @param fanId the fan's ID
     * @return total price of all items in the cart
     */
    public double getCartTotal(String fanId) {
        return shoppingCartService.getCartTotal(fanId);
    }

    /**
     * Processes checkout for the given fan.
     *
     * @param fanId       the fan's ID
     * @param processedBy username of the staff processing (or "SELF" for fan)
     * @return the transaction record
     * @throws InsufficientBalanceException if the fan cannot afford the total
     */
    public Transaction checkout(String fanId, String processedBy) throws IOException {
        return shoppingCartService.checkout(fanId, processedBy);
    }

    /**
     * Removes an item from the cart by its cart item ID.
     *
     * @param cartItemId the cart item ID to remove
     * @throws IOException if persistence fails
     */
    public void removeFromCart(String cartItemId) throws IOException {
        shoppingCartService.removeFromCart(cartItemId);
    }

    /**
     * Clears all items from a fan's cart.
     *
     * @param fanId the fan's ID
     * @throws IOException if persistence fails
     */
    public void clearCart(String fanId) throws IOException {
        shoppingCartService.clearCart(fanId);
    }
}
