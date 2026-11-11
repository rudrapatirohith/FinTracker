-- Add currency column to loans table if it doesn't exist
DO $$ 
BEGIN
    -- Check if currency column exists, if not add it
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loans' 
        AND column_name = 'currency' 
        AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.loans 
        ADD COLUMN currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));
        
        -- Update existing records to have INR as default currency
        UPDATE public.loans SET currency = 'INR' WHERE currency IS NULL;
        
        RAISE NOTICE 'Added currency column to loans table';
    ELSE
        RAISE NOTICE 'Currency column already exists in loans table';
    END IF;
END $$;

-- Add currency column to loan_payments table if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loan_payments' 
        AND column_name = 'currency' 
        AND table_schema = 'public'
    ) THEN
        ALTER TABLE public.loan_payments 
        ADD COLUMN currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));
        
        -- Update existing records to have INR as default currency
        UPDATE public.loan_payments SET currency = 'INR' WHERE currency IS NULL;
        
        RAISE NOTICE 'Added currency column to loan_payments table';
    ELSE
        RAISE NOTICE 'Currency column already exists in loan_payments table';
    END IF;
END $$;

-- Verify the schema changes
SELECT 
    table_name,
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns 
WHERE table_name IN ('loans', 'loan_payments') 
AND column_name = 'currency'
AND table_schema = 'public'
ORDER BY table_name, column_name;

-- Show current loans table structure
SELECT 
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'loans' 
AND table_schema = 'public'
ORDER BY ordinal_position;
