-- Add furigana and romaji fields to tasks
ALTER TABLE tasks ADD COLUMN furigana_text TEXT;
ALTER TABLE tasks ADD COLUMN romaji_text TEXT;
