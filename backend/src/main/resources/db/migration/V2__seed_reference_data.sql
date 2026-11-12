-- Dados de referencia (PostgreSQL) - idempotente.
--
-- A escola e a turma de demonstracao vivem aqui (e nao apenas no DemoDataSeeder) porque
-- `students.school_id` e `students.classroom_id` sao chaves estrangeiras: sem essas linhas o
-- cadastro de aluno falha com violacao de FK em 500. O seeder do perfil `demo` apenas usa
-- estes IDs fixos.
INSERT INTO schools (id, name, external_code)
VALUES ('11111111-1111-1111-1111-111111111111', 'Escola Municipal ODS 4', 'ESC-ODS4-001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO classrooms (id, school_id, name, grade, shift)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
        '6o Ano A', '6', 'MANHA')
ON CONFLICT (id) DO NOTHING;

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
