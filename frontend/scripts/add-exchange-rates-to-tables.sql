-- Add exchange rate columns to income table
ALTER TABLE income 
ADD COLUMN IF NOT EXISTS exchange_rate DECIMAL(10,6) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS inr_equivalent DECIMAL(15,2) DEFAULT NULL;

-- Add exchange rate columns to international_transfers table  
ALTER TABLE international_transfers
ADD COLUMN IF NOT EXISTS usd_to_inr_rate DECIMAL(10,6) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS inr_equivalent DECIMAL(15,2) DEFAULT NULL;

-- Add exchange rate columns to loan_payments table
ALTER TABLE loan_payments
ADD COLUMN IF NOT EXISTS exchange_rate DECIMAL(10,6) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS inr_equivalent DECIMAL(15,2) DEFAULT NULL;

-- Create monthly_expenses table for expense tracking
CREATE TABLE IF NOT EXISTS monthly_expenses (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  expense_name VARCHAR(255) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  exchange_rate DECIMAL(10,6) DEFAULT NULL,
  inr_equivalent DECIMAL(15,2) DEFAULT NULL,
  category VARCHAR(100),
  expense_date DATE NOT NULL,
  payment_method VARCHAR(100),
  notes TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on monthly_expenses
ALTER TABLE monthly_expenses ENABLE ROW LEVEL SECURITY;

-- Create policy for monthly_expenses
CREATE POLICY "Users can manage their own monthly expenses" ON monthly_expenses
  FOR ALL USING (auth.uid() = user_id);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_monthly_expenses_user_date ON monthly_expenses(user_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_monthly_expenses_category ON monthly_expenses(category);

-- Update existing records to have proper exchange rates (example rates)
-- Note: In production, you'd want to set these based on historical data
UPDATE income 
SET exchange_rate = 83.25, 
    inr_equivalent = amount * 83.25 
WHERE currency = 'USD' AND exchange_rate IS NULL;

UPDATE international_transfers 
SET usd_to_inr_rate = 83.25,
    inr_equivalent = amount_sent * 83.25 
WHERE currency_sent = 'USD' AND usd_to_inr_rate IS NULL;

UPDATE loan_payments 
SET exchange_rate = 83.25,
    inr_equivalent = amount * 83.25 
WHERE currency = 'USD' AND exchange_rate IS NULL;
