-- User XP and level tracking
CREATE TABLE user_xp (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    total_xp INTEGER DEFAULT 0,
    current_level INTEGER DEFAULT 1,
    last_streak_bonus_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial row (single-user system)
INSERT INTO user_xp (total_xp, current_level) VALUES (0, 1);

-- XP activity log for tracking gains
CREATE TABLE xp_activities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_type TEXT NOT NULL, -- 'task', 'session', 'streak', 'new_word'
    xp_gained INTEGER NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_xp_activities_created ON xp_activities(created_at);
