-- Vocabulary words imported from CSV files
CREATE TABLE words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    japanese TEXT NOT NULL,
    romaji TEXT NOT NULL,
    norwegian TEXT NOT NULL,
    category TEXT NOT NULL,
    category_name TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_words_category ON words(category);

-- Learning sessions
CREATE TABLE sessions (
    id TEXT PRIMARY KEY,
    difficulty TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    current_task_index INTEGER DEFAULT 0
);

-- Tasks within a session
CREATE TABLE tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL REFERENCES sessions(id),
    task_index INTEGER NOT NULL,
    task_type TEXT NOT NULL,
    japanese_text TEXT NOT NULL,
    norwegian_translation TEXT NOT NULL,
    word_ids TEXT NOT NULL,
    audio_hash TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(session_id, task_index)
);

CREATE INDEX idx_tasks_session ON tasks(session_id);

-- Word learning progress
CREATE TABLE word_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word_id INTEGER NOT NULL REFERENCES words(id),
    times_seen INTEGER DEFAULT 0,
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(word_id)
);

CREATE INDEX idx_word_progress_word ON word_progress(word_id);
