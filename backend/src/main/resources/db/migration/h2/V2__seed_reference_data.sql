-- Dados de referencia (H2): mesmas linhas da base PostgreSQL, em sintaxe H2.
INSERT INTO schools (id, name, external_code)
VALUES ('11111111-1111-1111-1111-111111111111', 'Escola Municipal ODS 4', 'ESC-ODS4-001');

INSERT INTO classrooms (id, school_id, name, grade, shift)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
        '6o Ano A', '6', 'MANHA');

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'camisa-time', 'Camisa do Time', 'OUTFIT', 20,
        '/assets/avatar/shirt.png');

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000002', 'chuteira-dourada', 'Chuteira Dourada', 'ACCESSORY', 30,
        '/assets/avatar/boots.png');

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000003', 'bone-aba-reta', 'Bone Aba Reta', 'ACCESSORY', 15,
        '/assets/avatar/cap.png');

INSERT INTO shop_items (id, code, name, category, price_coins, asset_url)
VALUES ('aaaaaaaa-0000-0000-0000-000000000004', 'fone-gamer', 'Fone Gamer', 'ACCESSORY', 25,
        '/assets/avatar/headset.png');
