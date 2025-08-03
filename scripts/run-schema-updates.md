# Database Schema Update Instructions

## Problem
The `currency` column is missing from the `loans` and `loan_payments` tables, causing the application to fail when trying to access loan data.

## Solution
Run the following SQL scripts in your Supabase SQL Editor:

### Step 1: Quick Fix (Run this first)
\`\`\`sql
-- Add currency column to loans table
ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));

-- Add currency column to loan_payments table  
ALTER TABLE public.loan_payments ADD COLUMN IF NOT EXISTS currency TEXT DEFAULT 'INR' CHECK (currency IN ('INR', 'USD'));

-- Update existing records
UPDATE public.loans SET currency = 'INR' WHERE currency IS NULL;
UPDATE public.loan_payments SET currency = 'INR' WHERE currency IS NULL;
\`\`\`

### Step 2: Complete Schema Verification (Optional)
Run the `verify-and-fix-schema.sql` script for a complete schema check and fix.

### Step 3: Verify Changes
\`\`\`sql
-- Check if columns were added successfully
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name IN ('loans', 'loan_payments') 
AND column_name = 'currency' 
AND table_schema = 'public';
\`\`\`

## Expected Result
After running these scripts, you should see:
- `loans.currency` column with default 'INR'
- `loan_payments.currency` column with default 'INR'
- All existing records updated with 'INR' currency
- Application should work without currency column errors

## How to Run
1. Go to your Supabase Dashboard
2. Navigate to SQL Editor
3. Create a new query
4. Copy and paste the Step 1 SQL code
5. Click "Run" button
6. Verify the changes with the verification query

The application should work immediately after running Step 1.
