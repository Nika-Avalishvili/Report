-- liquibase formatted sql

-- changeset nika.avalishvili:1
ALTER TABLE report_entry DROP COLUMN accrual_date;

-- changeset nika.avalishvili:2
ALTER TABLE report_entry
    ADD COLUMN document_id INT
    REFERENCES documents
    ON DELETE SET NULL

