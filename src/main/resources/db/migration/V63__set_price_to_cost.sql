-- price column now stores the cost/purchase price
-- selling_price already holds the selling price (set in V62)

UPDATE vehicles SET price = COALESCE(purchase_price, 0);
