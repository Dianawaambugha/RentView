.bail on
PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

PRAGMA defer_foreign_keys = ON;

CREATE TABLE users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    approval_status TEXT NOT NULL DEFAULT 'PENDING'
);

INSERT INTO users_new (
    id, full_name, email, phone, password_hash, role, approval_status
)
SELECT
    id, full_name, email, phone, password_hash, role, approval_status
FROM users;

DROP TABLE users;
ALTER TABLE users_new RENAME TO users;

CREATE UNIQUE INDEX index_users_email ON users(email);
CREATE INDEX index_properties_owner_id ON properties(owner_id);
CREATE INDEX index_property_photos_property_id ON property_photos(property_id);
CREATE INDEX index_property_edit_requests_property_id ON property_edit_requests(property_id);
CREATE INDEX index_property_edit_photos_edit_id ON property_edit_photos(edit_id);
CREATE INDEX index_viewings_tenant_id ON viewings(tenant_id);

COMMIT;
