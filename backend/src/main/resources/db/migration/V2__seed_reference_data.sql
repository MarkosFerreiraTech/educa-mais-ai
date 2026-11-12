-- Dados de referencia minimos (itens da loja) - idempotente.
INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'camisa-time', 'Camisa do Time', 'OUTFIT', 20,
        '/assets/avatar/shirt.png')
ON CONFLICT (code) DO NOTHING;

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000002', 'chuteira-dourada', 'Chuteira Dourada', 'ACCESSORY', 30,
        '/assets/avatar/boots.png')
ON CONFLICT (code) DO NOTHING;

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000003', 'bone-aba-reta', 'Bone Aba Reta', 'ACCESSORY', 15,
        '/assets/avatar/cap.png')
ON CONFLICT (code) DO NOTHING;

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000004', 'fone-gamer', 'Fone Gamer', 'ACCESSORY', 25,
        '/assets/avatar/headset.png')
ON CONFLICT (code) DO NOTHING;
