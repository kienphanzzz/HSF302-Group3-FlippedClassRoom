USE EduNextV2;
GO

-- 1. Add fields to practice_sessions table
IF COL_LENGTH('practice_sessions', 'source_type') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD source_type NVARCHAR(50) NULL;
END

IF COL_LENGTH('practice_sessions', 'source_title') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD source_title NVARCHAR(255) NULL;
END

IF COL_LENGTH('practice_sessions', 'source_text') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD source_text NVARCHAR(MAX) NULL;
END

IF COL_LENGTH('practice_sessions', 'source_url') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD source_url NVARCHAR(1000) NULL;
END

IF COL_LENGTH('practice_sessions', 'uploaded_file_path') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD uploaded_file_path NVARCHAR(1000) NULL;
END

IF COL_LENGTH('practice_sessions', 'original_file_name') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD original_file_name NVARCHAR(255) NULL;
END

IF COL_LENGTH('practice_sessions', 'custom_prompt') IS NULL
BEGIN
    ALTER TABLE practice_sessions ADD custom_prompt NVARCHAR(MAX) NULL;
END

-- Make content_id nullable in practice_sessions
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('practice_sessions')
      AND name = 'content_id'
      AND is_nullable = 0
)
BEGIN
    ALTER TABLE practice_sessions ALTER COLUMN content_id BIGINT NULL;
END

-- 2. Add options to practice_questions table
IF COL_LENGTH('practice_questions', 'option_a') IS NULL
BEGIN
    ALTER TABLE practice_questions ADD option_a NVARCHAR(1000) NULL;
END

IF COL_LENGTH('practice_questions', 'option_b') IS NULL
BEGIN
    ALTER TABLE practice_questions ADD option_b NVARCHAR(1000) NULL;
END

IF COL_LENGTH('practice_questions', 'option_c') IS NULL
BEGIN
    ALTER TABLE practice_questions ADD option_c NVARCHAR(1000) NULL;
END

IF COL_LENGTH('practice_questions', 'option_d') IS NULL
BEGIN
    ALTER TABLE practice_questions ADD option_d NVARCHAR(1000) NULL;
END
GO
