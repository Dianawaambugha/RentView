PRAGMA foreign_keys = ON;

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    approval_status TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE properties (
    property_id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    location TEXT NOT NULL,
    rent REAL NOT NULL,
    bedrooms INTEGER NOT NULL,
    description TEXT NOT NULL,
    property_status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(owner_id) REFERENCES users(id)
);

CREATE TABLE property_photos (
    photo_id INTEGER PRIMARY KEY AUTOINCREMENT,
    property_id INTEGER NOT NULL,
    photo_uri TEXT NOT NULL,
    FOREIGN KEY(property_id) REFERENCES properties(property_id) ON DELETE CASCADE
);

CREATE TABLE property_edit_requests (
    edit_id INTEGER PRIMARY KEY AUTOINCREMENT,
    property_id INTEGER NOT NULL,
    proposed_title TEXT NOT NULL,
    proposed_location TEXT NOT NULL,
    proposed_rent REAL NOT NULL,
    proposed_bedrooms INTEGER NOT NULL,
    proposed_description TEXT NOT NULL,
    edit_status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(property_id) REFERENCES properties(property_id) ON DELETE CASCADE
);

CREATE TABLE property_edit_photos (
    edit_photo_id INTEGER PRIMARY KEY AUTOINCREMENT,
    edit_id INTEGER NOT NULL,
    photo_uri TEXT NOT NULL,
    FOREIGN KEY(edit_id) REFERENCES property_edit_requests(edit_id) ON DELETE CASCADE
);

CREATE TABLE viewings (
    viewing_id INTEGER PRIMARY KEY AUTOINCREMENT,
    property_id INTEGER NOT NULL,
    tenant_id INTEGER NOT NULL,
    viewing_date TEXT NOT NULL,
    viewing_slot TEXT NOT NULL,
    FOREIGN KEY(property_id) REFERENCES properties(property_id),
    FOREIGN KEY(tenant_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX idx_viewing_unique_slot
ON viewings(property_id, viewing_date, viewing_slot);

INSERT INTO users
(id, full_name, email, phone, password_hash, role, approval_status)
VALUES
(1, 'Test Owner', 'owner@test.com', '0700000001', 'test_hash_1', 'Owner', 'APPROVED'),
(2, 'Test Tenant', 'tenant@test.com', '0700000002', 'test_hash_2', 'Tenant', 'APPROVED');

INSERT INTO properties
(property_id, owner_id, title, location, rent, bedrooms, description, property_status)
VALUES
(101, 1, 'Test Apartment', 'Nairobi', 25000, 2, 'Migration test property', 'APPROVED');

INSERT INTO property_photos
(photo_id, property_id, photo_uri)
VALUES
(151, 101, 'content://test/photo1');

INSERT INTO property_edit_requests
(edit_id, property_id, proposed_title, proposed_location, proposed_rent,
 proposed_bedrooms, proposed_description, edit_status)
VALUES
(201, 101, 'Updated Test Apartment', 'Nairobi', 27000, 2,
 'Updated migration test property', 'PENDING');

INSERT INTO property_edit_photos
(edit_photo_id, edit_id, photo_uri)
VALUES
(251, 201, 'content://test/editphoto1');

INSERT INTO viewings
(viewing_id, property_id, tenant_id, viewing_date, viewing_slot)
VALUES
(301, 101, 2, '2026-11-15', '10:00 AM');

PRAGMA user_version = 5;
