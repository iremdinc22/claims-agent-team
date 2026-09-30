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
    CONSTRAINT claims_policy_fk FOREIGN KEY (policy_number) REFERENCES policies (policy_number),
    CONSTRAINT claims_incident_type_allowed CHECK (
        incident_type IN ('COLLISION', 'THEFT', 'GLASS_DAMAGE', 'OTHER')
    ),
    CONSTRAINT claims_description_non_blank CHECK (btrim(description) <> ''),
    CONSTRAINT claims_status_reported CHECK (status = 'REPORTED')
);
