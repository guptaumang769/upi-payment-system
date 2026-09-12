-- Seed wallets so the demo can transfer money end to end without manual setup.
INSERT INTO wallets (vpa, balance, version) VALUES
    ('alice@upi', 10000.00, 0),
    ('bob@upi',    5000.00, 0),
    ('carol@upi',     0.00, 0);
