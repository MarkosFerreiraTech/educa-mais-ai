-- ============================================================
-- Educa Mais - esquema base (PostgreSQL 16)
-- PII cifrada na aplicacao (AES-256-GCM): colunas *_enc
-- ============================================================

CREATE TABLE schools (
    id            UUID PRIMARY KEY,
    name          VARCHAR(160) NOT NULL,
    external_code VARCHAR(60) UNIQUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE classrooms (
    id         UUID PRIMARY KEY,
    school_id  UUID NOT NULL REFERENCES schools (id),
    teacher_id UUID,
    name       VARCHAR(80) NOT NULL,
    grade      VARCHAR(40),
    shift      VARCHAR(20)
);

CREATE TABLE students (
    id                  UUID PRIMARY KEY,
    school_id           UUID REFERENCES schools (id),
    classroom_id        UUID REFERENCES classrooms (id),
    name_enc            VARCHAR(512) NOT NULL,
    email_enc           VARCHAR(512) NOT NULL,
    email_hash          VARCHAR(128) NOT NULL UNIQUE,
    phone_enc           VARCHAR(512),
    birth_date_enc      VARCHAR(512),
    password_hash       VARCHAR(255) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    notification_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    interests           VARCHAR(512),
    version             BIGINT DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_students_classroom ON students (classroom_id);

CREATE TABLE lessons (
    id                  UUID PRIMARY KEY,
    classroom_id        UUID REFERENCES classrooms (id),
    subject             VARCHAR(120) NOT NULL,
    topic               VARCHAR(240) NOT NULL,
    lesson_date         DATE NOT NULL,
    learning_objectives VARCHAR(2000),
    topics              VARCHAR(2000),
    prerequisites       VARCHAR(2000),
    source_reference    VARCHAR(240)
);
CREATE INDEX idx_lessons_classroom_date ON lessons (classroom_id, lesson_date DESC);

CREATE TABLE attendances (
    id          UUID PRIMARY KEY,
    student_id  UUID NOT NULL REFERENCES students (id),
    lesson_id   UUID NOT NULL REFERENCES lessons (id),
    event_id    VARCHAR(120) NOT NULL UNIQUE,
    status      VARCHAR(24) NOT NULL,
    origin      VARCHAR(24) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_attendances_student ON attendances (student_id, occurred_at DESC);

CREATE TABLE learning_missions (
    id             UUID PRIMARY KEY,
    attendance_id  UUID NOT NULL UNIQUE REFERENCES attendances (id),
    student_id     UUID NOT NULL REFERENCES students (id),
    lesson_id      UUID NOT NULL REFERENCES lessons (id),
    mission_status VARCHAR(32) NOT NULL,
    plan_source    VARCHAR(16) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at   TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_missions_student_status ON learning_missions (student_id, mission_status);

CREATE TABLE mission_steps (
    id          UUID PRIMARY KEY,
    mission_id  UUID NOT NULL REFERENCES learning_missions (id) ON DELETE CASCADE,
    step_order  INTEGER NOT NULL,
    objective   VARCHAR(500) NOT NULL,
    explanation VARCHAR(4000),
    example     VARCHAR(2000),
    strategy    VARCHAR(32) NOT NULL,
    step_status VARCHAR(32) NOT NULL,
    attempts    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_steps_mission ON mission_steps (mission_id, step_order);

CREATE TABLE diagnostic_questions (
    id                 UUID PRIMARY KEY,
    mission_id         UUID NOT NULL REFERENCES learning_missions (id) ON DELETE CASCADE,
    step_id            UUID NOT NULL,
    prompt             VARCHAR(1000) NOT NULL,
    question_type      VARCHAR(24) NOT NULL,
    options            VARCHAR(2000),
    correct_answer_enc VARCHAR(512),
    explanation        VARCHAR(2000),
    difficulty         INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX idx_questions_mission ON diagnostic_questions (mission_id);

CREATE TABLE diagnostic_attempts (
    id                 UUID PRIMARY KEY,
    question_id        UUID NOT NULL REFERENCES diagnostic_questions (id),
    student_id         UUID NOT NULL REFERENCES students (id),
    answer_enc         VARCHAR(1000),
    classification     VARCHAR(16) NOT NULL,
    feedback           VARCHAR(2000),
    answered_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    time_spent_seconds INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_attempts_question_student ON diagnostic_attempts (question_id, student_id);

CREATE TABLE gamification_profiles (
    student_id       UUID PRIMARY KEY REFERENCES students (id),
    total_xp         INTEGER NOT NULL DEFAULT 0,
    diamonds         INTEGER NOT NULL DEFAULT 0,
    coins            INTEGER NOT NULL DEFAULT 0,
    current_streak   INTEGER NOT NULL DEFAULT 0,
    longest_streak   INTEGER NOT NULL DEFAULT 0,
    last_streak_date DATE
);

CREATE TABLE reward_ledger (
    id              UUID PRIMARY KEY,
    student_id      UUID NOT NULL REFERENCES students (id),
    mission_id      UUID NOT NULL REFERENCES learning_missions (id),
    reward_type     VARCHAR(16) NOT NULL,
    amount          INTEGER NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    granted_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE shop_items (
    id          UUID PRIMARY KEY,
    code        VARCHAR(80) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    category    VARCHAR(40) NOT NULL,
    price_coins INTEGER NOT NULL,
    asset_url   VARCHAR(240)
);

CREATE TABLE avatar_items_owned (
    id          UUID PRIMARY KEY,
    student_id  UUID NOT NULL REFERENCES students (id),
    item_code   VARCHAR(80) NOT NULL,
    acquired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_owned_student_item UNIQUE (student_id, item_code)
);

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY,
    student_id          UUID NOT NULL REFERENCES students (id),
    mission_id          UUID NOT NULL REFERENCES learning_missions (id),
    channel             VARCHAR(16) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    provider_message_id VARCHAR(120),
    failure_reason      VARCHAR(240),
    payload             VARCHAR(2000),
    sent_at             TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_notifications_status_channel ON notifications (status, channel);

CREATE TABLE teacher_reports (
    id              UUID PRIMARY KEY,
    mission_id      UUID NOT NULL UNIQUE REFERENCES learning_missions (id),
    student_id      UUID NOT NULL REFERENCES students (id),
    teacher_id      UUID,
    summary         VARCHAR(2000),
    identified_gaps VARCHAR(2000),
    recommendation  VARCHAR(2000),
    success_rate    INTEGER NOT NULL DEFAULT 0,
    delivery_status VARCHAR(24) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agent_audit_log (
    id         UUID PRIMARY KEY,
    mission_id UUID,
    action     VARCHAR(16) NOT NULL,
    decision   VARCHAR(500),
    details    VARCHAR(4000),
    latency_ms INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_mission ON agent_audit_log (mission_id, created_at);

CREATE TABLE security_audit_log (
    id         UUID PRIMARY KEY,
    event      VARCHAR(120) NOT NULL,
    subject_ref VARCHAR(120),
    detail     VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
