INSERT INTO policies (policy_number)
VALUES ('MOTOR-POLICY-001')
ON CONFLICT (policy_number) DO NOTHING;
