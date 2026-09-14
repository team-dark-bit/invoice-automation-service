-- V8: correct the BCrypt password for the local user seeded by V7.
-- Local development credentials: haroldqc / password
UPDATE users
SET "password" = '$2a$10$yGHbWO6O70CkV2pQHxDfgutWehzGB5kt7zVrPSLAg34cJkcQKWtSq'
WHERE id = 'bb943bde-99df-5229-b50e-3647631cdc43'
  AND username = 'haroldqc';
