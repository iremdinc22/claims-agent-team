BEGIN;

CREATE TABLE IF NOT EXISTS policies (
    policy_number text PRIMARY KEY,
    CONSTRAINT policies_policy_number_non_blank CHECK (btrim(policy_number) <> '')
);

CREATE TABLE IF NOT EXISTS claims (
    claim_number uuid PRIMARY KEY,
    policy_number text NOT NULL,
    incident_type text NOT NULL,
    incident_date date NOT NULL,
    description text NOT NULL,
    status text NOT NULL,
    created_by text,
    created_at timestamptz,
    CONSTRAINT claims_policy_fk FOREIGN KEY (policy_number) REFERENCES policies (policy_number),
    CONSTRAINT claims_incident_type_allowed CHECK (
        incident_type IN ('COLLISION', 'THEFT', 'GLASS_DAMAGE', 'OTHER')
    ),
    CONSTRAINT claims_description_non_blank CHECK (btrim(description) <> ''),
    CONSTRAINT claims_status_allowed CHECK (status IN ('REPORTED', 'PENDING', 'APPROVED', 'REJECTED'))
);

ALTER TABLE claims ADD COLUMN IF NOT EXISTS created_by text;
ALTER TABLE claims ADD COLUMN IF NOT EXISTS created_at timestamptz;

UPDATE claims SET created_by = COALESCE(created_by, 'prototype-demo-user'),
    created_at = COALESCE(created_at, transaction_timestamp())
WHERE created_by IS NULL OR created_at IS NULL;

ALTER TABLE claims ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE claims ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE claims DROP CONSTRAINT IF EXISTS claims_status_reported;
-- Single-quoted DO body keeps embedded semicolons intact in Spring SQL initialization.
DO '
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
        WHERE conrelid = ''claims''::regclass AND conname = ''claims_status_allowed'') THEN
        ALTER TABLE claims ADD CONSTRAINT claims_status_allowed
            CHECK (status IN (''REPORTED'', ''PENDING'', ''APPROVED'', ''REJECTED''));
    END IF;
END ';
ALTER TABLE claims VALIDATE CONSTRAINT claims_status_allowed;
CREATE INDEX IF NOT EXISTS claims_owner_incident_creation_idx
    ON claims (created_by, incident_date DESC, created_at DESC, claim_number ASC);
COMMIT;
