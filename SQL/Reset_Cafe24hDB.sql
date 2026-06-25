/*
    Use this script only when the database must be recreated.
    It permanently deletes all data in Cafe24hDB.
*/

USE master;
GO

IF DB_ID(N'Cafe24hDB') IS NOT NULL
BEGIN
    ALTER DATABASE Cafe24hDB
    SET SINGLE_USER
    WITH ROLLBACK IMMEDIATE;

    DROP DATABASE Cafe24hDB;
END;
GO

SELECT N'Cafe24hDB đã được xóa. Có thể chạy lại Cafe24hDB.sql.' AS KetQua;
GO
