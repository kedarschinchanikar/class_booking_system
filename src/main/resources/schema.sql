-- Connect to PostgreSQL
-- Schema for Class Booking System

CREATE TABLE IF NOT EXISTS teachers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    timezone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS parents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    timezone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS offerings (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id),
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    name VARCHAR(255) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 30,
    current_enrollment INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    offering_id BIGINT NOT NULL REFERENCES offerings(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_session_time CHECK (end_time > start_time)
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id),
    offering_id BIGINT NOT NULL REFERENCES offerings(id),
    booked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_parent_offering UNIQUE (parent_id, offering_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_sessions_offering ON sessions(offering_id);
CREATE INDEX IF NOT EXISTS idx_sessions_time ON sessions(start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_bookings_parent ON bookings(parent_id);
CREATE INDEX IF NOT EXISTS idx_bookings_offering ON bookings(offering_id);
CREATE INDEX IF NOT EXISTS idx_offerings_teacher ON offerings(teacher_id);
CREATE INDEX IF NOT EXISTS idx_offerings_course ON offerings(course_id);

-- Seed data for testing
INSERT INTO teachers (name, email, timezone) VALUES
    ('Alice Teacher', 'alice@example.com', 'America/New_York'),
    ('Bob Teacher', 'bob@example.com', 'Asia/Kolkata')
ON CONFLICT (email) DO NOTHING;

INSERT INTO parents (name, email, timezone) VALUES
    ('Charlie Parent', 'charlie@example.com', 'America/Los_Angeles'),
    ('Diana Parent', 'diana@example.com', 'Europe/London')
ON CONFLICT (email) DO NOTHING;

INSERT INTO courses (name, description) VALUES
    ('Minecraft Coding', 'Learn to code with Minecraft'),
    ('Art Drawing Class', 'Creative art drawing for kids'),
    ('Python Programming', 'Introduction to Python')
ON CONFLICT DO NOTHING;