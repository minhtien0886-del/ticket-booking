package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link Merchandise} entities backed by merchandise.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class MerchandiseRepository extends GenericCsvRepository<Merchandise> {

    private static final String HEADER = "productId,name,description,category,basePrice,size,color,stockQuantity,active,imageUrl";

    public MerchandiseRepository(Path dataDir) {
        super(
            dataDir.resolve("merchandise.csv"),
            "productId",
            Merchandise::getProductId,
            Merchandise::fromCsv,
            Merchandise::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<Merchandise> findByCategory(ProductCategory category) {
        return findAll(m -> m.getCategory() == category);
    }

    public List<Merchandise> findActive() {
        return findAll(Merchandise::isActive);
    }

    public List<Merchandise> findInStock() {
        return findAll(Merchandise::isInStock);
    }

    public List<Merchandise> searchByName(String keyword) {
        String k = keyword.toLowerCase();
        return findAll(m -> m.getName() != null && m.getName().toLowerCase().contains(k));
    }

    public void updateStock(String productId, int quantity) throws IOException {
        Merchandise m = findById(productId);
        if (m != null) {
            m.setStockQuantity(quantity);
            save(m);
        }
    }

    public void decrementStock(String productId, int quantity) throws IOException {
        Merchandise m = findById(productId);
        if (m != null) {
            m.setStockQuantity(Math.max(0, m.getStockQuantity() - quantity));
            save(m);
        }
    }

    @Override
    protected Merchandise parse(String csvLine) {
        return Merchandise.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Merchandise entity) {
        return entity.toCsv();
    }
}
