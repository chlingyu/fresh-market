-- Fix remaining schema mismatches between entities and database tables
-- Version: 1.0.0 (Fixed)
-- Date: 2025-09-05

-- Only add the notes column to orders table (other columns already exist correctly)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='orders' AND column_name='notes' AND table_schema=DATABASE()) = 0,
    'ALTER TABLE orders ADD COLUMN notes TEXT AFTER remark',
    'SELECT ''notes column already exists in orders'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;