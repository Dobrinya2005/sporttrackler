-- =====================================================
-- FITNESS TRAINER APP — DATABASE CREATION
-- MSSQL Server
-- =====================================================

USE master;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = 'FitnessTrainerDB')
BEGIN
    ALTER DATABASE FitnessTrainerDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE FitnessTrainerDB;
END
GO

CREATE DATABASE FitnessTrainerDB
    COLLATE Cyrillic_General_CI_AS;
GO

USE FitnessTrainerDB;
GO
