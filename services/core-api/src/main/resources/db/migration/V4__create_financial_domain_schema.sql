-- V4: Create the financial domain schema.
--
-- Financial records belong to app_users, not auth.users.
-- Existing Supabase user IDs are mapped explicitly through
-- legacy_user_mappings during the future data migration.
--
-- V1 and V2 remain unchanged.

-- ---------------------------------------------------------------------
-- Legacy identity mapping
-- ---------------------------------------------------------------------

CREATE TABLE legacy_user_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    provider VARCHAR(30) NOT NULL,
    legacy_user_id UUID NOT NULL,
    app_user_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_legacy_user_mappings_app_user
        FOREIGN KEY (app_user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_legacy_user_mappings_external
        UNIQUE (provider, legacy_user_id),

    CONSTRAINT uq_legacy_user_mappings_app_user
        UNIQUE (provider, app_user_id),

    CONSTRAINT chk_legacy_user_mappings_provider
        CHECK (provider IN ('SUPABASE'))
);

-- ---------------------------------------------------------------------
-- Income
-- ---------------------------------------------------------------------

CREATE TABLE income (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    source TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency TEXT DEFAULT 'USD',

    category TEXT,
    description TEXT,

    date DATE NOT NULL,

    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_frequency TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    exchange_rate NUMERIC(10, 6),
    inr_equivalent NUMERIC(15, 2),

    CONSTRAINT fk_income_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- International transfers
-- Both historical currency pairs are retained until data reconciliation.
-- ---------------------------------------------------------------------

CREATE TABLE international_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    recipient_name TEXT NOT NULL,
    recipient_country TEXT NOT NULL,

    amount_sent NUMERIC(12, 2) NOT NULL,
    currency_sent TEXT DEFAULT 'USD',

    amount_received NUMERIC(12, 2) NOT NULL,
    currency_received TEXT DEFAULT 'INR',

    exchange_rate NUMERIC(10, 6) NOT NULL,

    transfer_fee NUMERIC(12, 2) DEFAULT 0,

    transfer_method TEXT,
    purpose TEXT,
    status TEXT DEFAULT 'pending',

    transfer_date DATE NOT NULL,
    completion_date DATE,

    reference_number TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    sent_currency TEXT DEFAULT 'USD',
    received_currency TEXT DEFAULT 'INR',

    usd_to_inr_rate NUMERIC(10, 6),
    inr_equivalent NUMERIC(15, 2),

    CONSTRAINT fk_international_transfers_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_international_transfers_sent_currency
        CHECK (sent_currency IN ('INR', 'USD', 'EUR', 'GBP', 'CAD', 'AUD')),

    CONSTRAINT chk_international_transfers_received_currency
        CHECK (received_currency IN ('INR', 'USD', 'EUR', 'GBP', 'CAD', 'AUD'))
);

-- ---------------------------------------------------------------------
-- Loans
-- ---------------------------------------------------------------------

CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    loan_name TEXT NOT NULL,

    principal_amount NUMERIC(12, 2) NOT NULL,
    current_balance NUMERIC(12, 2) NOT NULL,

    interest_rate NUMERIC(5, 2),
    monthly_payment NUMERIC(12, 2),

    start_date DATE NOT NULL,
    end_date DATE,

    lender TEXT,
    loan_type TEXT,
    status TEXT DEFAULT 'active',

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    currency TEXT NOT NULL DEFAULT 'INR',

    CONSTRAINT fk_loans_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    -- Required as the referenced key for the composite loan-payment FK.
    CONSTRAINT uq_loans_id_user_id
        UNIQUE (id, user_id),

    CONSTRAINT chk_loans_currency
        CHECK (currency IN ('INR', 'USD'))
);

-- ---------------------------------------------------------------------
-- Loan payments
-- Composite FK ensures the payment and loan belong to the same user.
-- ---------------------------------------------------------------------

CREATE TABLE loan_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    loan_id UUID NOT NULL,
    user_id UUID NOT NULL,

    amount NUMERIC(12, 2) NOT NULL,
    principal_amount NUMERIC(12, 2),
    interest_amount NUMERIC(12, 2),

    payment_date DATE NOT NULL,

    payment_method TEXT,
    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),

    currency TEXT NOT NULL DEFAULT 'INR',
    exchange_rate NUMERIC(10, 6),
    inr_equivalent NUMERIC(15, 2),

    CONSTRAINT fk_loan_payments_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_loan_payments_loan_owner
        FOREIGN KEY (loan_id, user_id)
        REFERENCES loans(id, user_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_loan_payments_currency
        CHECK (currency IN ('INR', 'USD'))
);

-- ---------------------------------------------------------------------
-- Monthly expenses
-- ---------------------------------------------------------------------

CREATE TABLE monthly_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    expense_name VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    exchange_rate NUMERIC(10, 6),
    inr_equivalent NUMERIC(15, 2),

    category VARCHAR(100),

    expense_date DATE NOT NULL,

    payment_method VARCHAR(100),
    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT fk_monthly_expenses_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Scheduled payments
-- ---------------------------------------------------------------------

CREATE TABLE scheduled_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    payment_name TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency TEXT DEFAULT 'USD',

    category TEXT,
    recipient TEXT,

    due_date DATE NOT NULL,

    frequency TEXT,
    is_recurring BOOLEAN DEFAULT FALSE,
    auto_pay BOOLEAN DEFAULT FALSE,

    status TEXT DEFAULT 'pending',
    reminder_days INTEGER DEFAULT 3,

    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT fk_scheduled_payments_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Transactions
-- tags remains a PostgreSQL text array.
-- reference_id intentionally has no foreign key, matching the source.
-- ---------------------------------------------------------------------

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    type TEXT NOT NULL,
    category TEXT NOT NULL,
    subcategory TEXT,

    amount NUMERIC(12, 2) NOT NULL,
    currency TEXT DEFAULT 'USD',

    description TEXT,
    merchant TEXT,

    transaction_date DATE NOT NULL,

    payment_method TEXT,
    reference_id UUID,
    tags TEXT[],

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Indexes for user-scoped queries and date-based access patterns.
-- ---------------------------------------------------------------------

CREATE INDEX idx_income_user_date
    ON income(user_id, date DESC);

CREATE INDEX idx_international_transfers_user_date
    ON international_transfers(user_id, transfer_date DESC);

CREATE INDEX idx_loans_user_created
    ON loans(user_id, created_at DESC);

CREATE INDEX idx_loan_payments_user_date
    ON loan_payments(user_id, payment_date DESC);

CREATE INDEX idx_loan_payments_loan_user_date
    ON loan_payments(loan_id, user_id, payment_date DESC);

CREATE INDEX idx_monthly_expenses_user_date
    ON monthly_expenses(user_id, expense_date DESC);

CREATE INDEX idx_scheduled_payments_user_due_date
    ON scheduled_payments(user_id, due_date);

CREATE INDEX idx_transactions_user_date
    ON transactions(user_id, transaction_date DESC);
