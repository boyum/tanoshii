-- Add word_translations column to tasks table for storing LLM-generated translations
ALTER TABLE tasks ADD COLUMN word_translations TEXT;
