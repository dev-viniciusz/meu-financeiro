-- Roda toda vez que o servidor inicia. "IF NOT EXISTS" evita erro se as tabelas já existem.

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sessions (
    token_hash TEXT PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS fixed_expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    description  TEXT NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    category     TEXT NOT NULL,
    due_date     DATE NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- type: 'income' (renda), 'variable' (despesa avulsa) ou 'fixed' (ocorrência mensal de uma despesa fixa)
CREATE TABLE IF NOT EXISTS transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type             TEXT NOT NULL CHECK (type IN ('income', 'variable', 'fixed')),
    description      TEXT NOT NULL,
    amount_cents     BIGINT NOT NULL CHECK (amount_cents > 0),
    category         TEXT NOT NULL DEFAULT '',
    date             DATE NOT NULL,
    fixed_expense_id UUID REFERENCES fixed_expenses(id) ON DELETE CASCADE,
    occurrence_month DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (fixed_expense_id, occurrence_month)
);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions (user_id, date);

CREATE TABLE IF NOT EXISTS notes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    content    TEXT NOT NULL,
    note_date  DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
