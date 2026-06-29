USE EduNextV2;
GO

-- 1. Add columns to practice_questions if missing
IF COL_LENGTH('practice_questions', 'answered_at') IS NULL
BEGIN
    ALTER TABLE practice_questions ADD answered_at DATETIME2 NULL;
END

IF COL_LENGTH('practice_questions', 'revealed') IS NULL
BEGIN
    ALTER TABLE practice_questions ADD revealed BIT NULL;
END

-- 2. Add status column to practice_sessions if missing
IF COL_LENGTH('practice_sessions', 'status') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD status NVARCHAR(50) NOT NULL DEFAULT 'GENERATED';
END

-- 3. Create practice_question_chats table if not exists
IF OBJECT_ID('practice_question_chats', 'U') IS NULL
BEGIN
    CREATE TABLE practice_question_chats (
        chat_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        practice_question_id BIGINT NOT NULL,
        practice_session_id BIGINT NOT NULL,
        student_id BIGINT NOT NULL,
        user_message NVARCHAR(MAX) NOT NULL,
        ai_response NVARCHAR(MAX) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_practice_question_chats_question FOREIGN KEY (practice_question_id) REFERENCES practice_questions(practice_question_id),
        CONSTRAINT FK_practice_question_chats_session FOREIGN KEY (practice_session_id) REFERENCES practice_sessions(practice_session_id),
        CONSTRAINT FK_practice_question_chats_student FOREIGN KEY (student_id) REFERENCES users(user_id)
    );
END
GO
