--:name arche/insert-trade
INSERT INTO trade (id, asset_basket) VALUES (:id, :asset-basket)

--:name arche/select-trade
SELECT id, :i:asset-basket FROM trade where id = :id
