-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create profiles table (extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    email TEXT,
    full_name TEXT,
    avatar_url TEXT,
    phone TEXT,
    date_of_birth DATE,
    address TEXT,
    city TEXT,
    country TEXT DEFAULT 'United States',
    postal_code TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create income table (always in USD)
CREATE TABLE IF NOT EXISTS public.income (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    source TEXT NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    currency TEXT DEFAULT 'USD' CHECK (currency = 'USD'), -- Always USD for income
    category TEXT,
    description TEXT,
    date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_frequency TEXT CHECK (recurring_frequency IN ('weekly', 'monthly', 'quarterly', 'yearly')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create loans table (supports INR and USD, but primarily INR)
CREATE TABLE IF NOT EXISTS public.loans (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    loan_name TEXT NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL CHECK (principal_amount > 0),
    current_balance DECIMAL(15,2) NOT NULL CHECK (current_balance >= 0),
    interest_rate DECIMAL(5,2) CHECK (interest_rate >= 0),
    monthly_payment DECIMAL(15,2) CHECK (monthly_payment > 0),
    start_date DATE NOT NULL,
    end_date DATE,
    lender TEXT,
    loan_type TEXT,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'paid_off', 'defaulted')),
    currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create loan_payments table
CREATE TABLE IF NOT EXISTS public.loan_payments (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    loan_id UUID REFERENCES public.loans(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    principal_amount DECIMAL(15,2) CHECK (principal_amount >= 0),
    interest_amount DECIMAL(15,2) CHECK (interest_amount >= 0),
    payment_date DATE NOT NULL,
    payment_method TEXT,
    notes TEXT,
    currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create international_transfers table (primarily USD with various receiving currencies)
CREATE TABLE IF NOT EXISTS public.international_transfers (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    recipient_name TEXT NOT NULL,
    recipient_country TEXT NOT NULL,
    amount_sent DECIMAL(15,2) NOT NULL CHECK (amount_sent > 0),
    currency_sent TEXT DEFAULT 'USD' CHECK (currency_sent IN ('USD', 'EUR', 'GBP', 'CAD', 'AUD')),
    amount_received DECIMAL(15,2) NOT NULL CHECK (amount_received > 0),
    currency_received TEXT NOT NULL CHECK (currency_received IN ('INR', 'EUR', 'GBP', 'CAD', 'AUD')),
    exchange_rate DECIMAL(10,6) NOT NULL CHECK (exchange_rate > 0),
    transfer_fee DECIMAL(15,2) DEFAULT 0 CHECK (transfer_fee >= 0),
    transfer_method TEXT,
    purpose TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'completed', 'failed')),
    transfer_date DATE NOT NULL,
    completion_date DATE,
    reference_number TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create scheduled_payments table
CREATE TABLE IF NOT EXISTS public.scheduled_payments (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    payment_name TEXT NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    currency TEXT DEFAULT 'USD' CHECK (currency IN ('USD', 'INR')),
    frequency TEXT NOT NULL CHECK (frequency IN ('weekly', 'monthly', 'quarterly', 'yearly')),
    next_payment_date DATE NOT NULL,
    recipient TEXT,
    category TEXT,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create transactions table (for general transaction history)
CREATE TABLE IF NOT EXISTS public.transactions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('income', 'expense', 'transfer', 'loan_payment')),
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    currency TEXT DEFAULT 'USD' CHECK (currency IN ('USD', 'INR')),
    description TEXT NOT NULL,
    category TEXT,
    reference_id UUID, -- Can reference income, loans, transfers, etc.
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON public.profiles(id);
CREATE INDEX IF NOT EXISTS idx_income_user_id ON public.income(user_id);
CREATE INDEX IF NOT EXISTS idx_income_date ON public.income(date);
CREATE INDEX IF NOT EXISTS idx_loans_user_id ON public.loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON public.loans(status);
CREATE INDEX IF NOT EXISTS idx_loan_payments_user_id ON public.loan_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_loan_payments_loan_id ON public.loan_payments(loan_id);
CREATE INDEX IF NOT EXISTS idx_transfers_user_id ON public.international_transfers(user_id);
CREATE INDEX IF NOT EXISTS idx_transfers_date ON public.international_transfers(transfer_date);
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user_id ON public.scheduled_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_next_date ON public.scheduled_payments(next_payment_date);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON public.transactions(transaction_date);

-- Enable Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.income ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loan_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.international_transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scheduled_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
-- Profiles policies
CREATE POLICY "Users can view own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

-- Income policies
CREATE POLICY "Users can view own income" ON public.income
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own income" ON public.income
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own income" ON public.income
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own income" ON public.income
    FOR DELETE USING (auth.uid() = user_id);

-- Loans policies
CREATE POLICY "Users can view own loans" ON public.loans
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own loans" ON public.loans
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own loans" ON public.loans
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own loans" ON public.loans
    FOR DELETE USING (auth.uid() = user_id);

-- Loan payments policies
CREATE POLICY "Users can view own loan payments" ON public.loan_payments
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own loan payments" ON public.loan_payments
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own loan payments" ON public.loan_payments
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own loan payments" ON public.loan_payments
    FOR DELETE USING (auth.uid() = user_id);

-- International transfers policies
CREATE POLICY "Users can view own transfers" ON public.international_transfers
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own transfers" ON public.international_transfers
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own transfers" ON public.international_transfers
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own transfers" ON public.international_transfers
    FOR DELETE USING (auth.uid() = user_id);

-- Scheduled payments policies
CREATE POLICY "Users can view own scheduled payments" ON public.scheduled_payments
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own scheduled payments" ON public.scheduled_payments
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own scheduled payments" ON public.scheduled_payments
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own scheduled payments" ON public.scheduled_payments
    FOR DELETE USING (auth.uid() = user_id);

-- Transactions policies
CREATE POLICY "Users can view own transactions" ON public.transactions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own transactions" ON public.transactions
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Create functions for updated_at timestamps
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER handle_updated_at_profiles
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER handle_updated_at_income
    BEFORE UPDATE ON public.income
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER handle_updated_at_loans
    BEFORE UPDATE ON public.loans
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER handle_updated_at_transfers
    BEFORE UPDATE ON public.international_transfers
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER handle_updated_at_scheduled_payments
    BEFORE UPDATE ON public.scheduled_payments
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- Insert sample data (optional - remove in production)
-- This will only work if you have a user authenticated
/*
INSERT INTO public.profiles (id, email, full_name, country) 
VALUES (auth.uid(), auth.email(), 'Sample User', 'United States')
ON CONFLICT (id) DO NOTHING;
*/
