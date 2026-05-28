-- PFBCOLOM.CL was delisted from Yahoo Finance; replace with GEB (Grupo Energía Bogotá)
UPDATE stock_snapshot
SET symbol = 'GEB',
    name   = 'Grupo Energía Bogotá',
    current_price  = 2830.00,
    previous_close = 2810.00,
    day_change     = 20.00,
    day_change_pct = 0.7117,
    volume         = 500000
WHERE symbol = 'PFBCOLOM';

-- CLH.CL was delisted from Yahoo Finance; replace with CORFICOLCF (Corporación Financiera Colombiana)
UPDATE stock_snapshot
SET symbol = 'CORFICOLCF',
    name   = 'Corporación Financiera Colombiana',
    current_price  = 15540.00,
    previous_close = 15400.00,
    day_change     = 140.00,
    day_change_pct = 0.9091,
    volume         = 120000
WHERE symbol = 'CLH';
