CREATE TABLE stock_snapshot (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol          VARCHAR(20)   UNIQUE NOT NULL,
    name            VARCHAR(100),
    current_price   DECIMAL(18,2),
    previous_close  DECIMAL(18,2),
    day_change      DECIMAL(18,2) NOT NULL DEFAULT 0,
    day_change_pct  DECIMAL(7,4)  NOT NULL DEFAULT 0,
    volume          BIGINT        NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

INSERT INTO stock_snapshot (symbol, name, current_price, previous_close, day_change, day_change_pct, volume) VALUES
    ('PFBCOLOM',  'Bancolombia Preferencial',  39500.00, 39150.00,  350.00,  0.8946, 1200000),
    ('NUTRESA',   'Grupo Nutresa',             68200.00, 68400.00, -200.00, -0.2924,  850000),
    ('ISA',       'Interconexión Eléctrica',   22100.00, 22000.00,  100.00,  0.4545,  600000),
    ('ECOPETROL', 'Ecopetrol S.A.',             1950.00,  1930.00,   20.00,  1.0363, 5000000),
    ('CEMARGOS',  'Cementos Argos',             7800.00,  7750.00,   50.00,  0.6452,  400000),
    ('GRUPOSURA', 'Grupo de Inversiones Suramericana', 18300.00, 18200.00, 100.00, 0.5495, 700000),
    ('EXITO',     'Almacenes Éxito',           13700.00, 13800.00, -100.00, -0.7246,  300000),
    ('ETB',       'Empresa de Telecomunicaciones de Bogotá', 410.00, 405.00, 5.00, 1.2346, 2000000),
    ('PFDAVVNDA', 'Banco Davivienda Preferencial', 52600.00, 52500.00, 100.00, 0.1905, 900000),
    ('CLH',       'Corporación Financiera Colombiana', 2350.00, 2300.00, 50.00, 2.1739, 150000);
