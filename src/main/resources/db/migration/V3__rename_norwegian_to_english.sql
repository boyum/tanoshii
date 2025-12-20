-- Rename norwegian column to english in words table
ALTER TABLE words RENAME COLUMN norwegian TO english;

-- Rename norwegian_translation column to english_translation in tasks table
ALTER TABLE tasks RENAME COLUMN norwegian_translation TO english_translation;
