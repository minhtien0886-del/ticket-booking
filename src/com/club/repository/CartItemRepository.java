package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link CartItem} entities backed by cart_items.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class CartItemRepository extends GenericCsvRepository<CartItem> {

    private static final String HEADER = "cartItemId,fanId,productId,productName,quantity,size,color,unitPrice,addedAt";

    public CartItemRepository(Path dataDir) {
        super(
            dataDir.resolve("cart_items.csv"),
            "cartItemId",
            CartItem::getCartItemId,
            CartItem::fromCsv,
            CartItem::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<CartItem> findByFanId(String fanId) {
        return findAll(ci -> fanId.equals(ci.getFanId()));
    }

    public void clearByFanId(String fanId) throws IOException {
        List<CartItem> items = findByFanId(fanId);
        synchronized (writeLock) {
            for (CartItem ci : items) {
                cache.remove(ci.getCartItemId());
            }
            saveAll();
        }
    }

    public double getCartTotal(String fanId) {
        return findByFanId(fanId).stream()
            .mapToDouble(CartItem::getSubtotal)
            .sum();
    }

    @Override
    protected CartItem parse(String csvLine) {
        return CartItem.fromCsv(csvLine);
    }

    @Override
    protected String serialize(CartItem entity) {
        return entity.toCsv();
    }
}
