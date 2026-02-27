-- Local development seed data (idempotent)
-- Usage (PowerShell + psql):
--   psql -h 127.0.0.1 -U neighborshare_app -d neighborshare -f docs/dev-seed.sql
--
-- If psql is unavailable, run equivalent SQL from pgAdmin query tool.

INSERT INTO apartments (
    id,
    name,
    invite_code,
    address,
    city,
    country,
    created_by
)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Demo Apartment',
    'DEMO123',
    'Demo Address',
    'Hyderabad',
    'India',
    '00000000-0000-0000-0000-000000000001'
)
ON CONFLICT (id) DO UPDATE
SET
    name = EXCLUDED.name,
    invite_code = EXCLUDED.invite_code,
    address = EXCLUDED.address,
    city = EXCLUDED.city,
    country = EXCLUDED.country;
