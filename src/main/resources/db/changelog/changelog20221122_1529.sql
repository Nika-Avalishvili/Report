-- liquibase formatted sql

-- changeset nika.avalishvili:1
CREATE TABLE report (id SERIAL PRIMARY KEY,
                    start_date DATE,
                    end_date DATE)


-- changeset nika.avalishvili:2
CREATE TABLE employee (id SERIAL PRIMARY KEY,
                      first_name VARCHAR(255),
                      last_name VARCHAR(255),
                      department VARCHAR(255),
                      positions VARCHAR(255),
                      email VARCHAR(255),
                      is_active Boolean,
                      is_pensions_payer Boolean)

-- changeset nika.avalishvili:3
CREATE TABLE benefit (id SERIAL PRIMARY KEY,
                       name VARCHAR(255),
                       benefit_type_name VARCHAR(255),
                       calculation_method_name VARCHAR(255))

-- changeset nika.avalishvili:4
CREATE TABLE documents (id SERIAL PRIMARY KEY,
                      upload_date DATE,
                      effective_date DATE,
                      employee_id INT,
                      benefit_id INT,
                      amount NUMERIC(19,2))

-- changeset nika.avalishvili:5
CREATE TABLE report_entry (id SERIAL PRIMARY KEY,
                        accrual_date DATE,
                        employee_id INT REFERENCES employee ON DELETE SET NULL,
                        benefit_id INT REFERENCES benefit ON DELETE SET NULL,
                        net_amount NUMERIC(19,2),
                        pensions_fund NUMERIC(19,2),
                        personal_income_tax NUMERIC(19,2),
                        gross_amount NUMERIC(19,2),
                        report_id INT REFERENCES report ON DELETE SET NULL)

