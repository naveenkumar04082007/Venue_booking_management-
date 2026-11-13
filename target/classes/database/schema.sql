
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT 1
);

CREATE TABLE IF NOT EXISTS venues (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    address TEXT NOT NULL,
    capacity INTEGER NOT NULL,
    hourly_price REAL,
    daily_price REAL,
    status VARCHAR(20) DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    user_id INTEGER,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER,
    venue_id INTEGER,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    total_amount REAL,
    advance_amount REAL,
    FOREIGN KEY(customer_id) REFERENCES customers(id),
    FOREIGN KEY(venue_id) REFERENCES venues(id)
);

-- Default Admin
INSERT OR IGNORE INTO users (username, password_hash, role, active) VALUES ('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'SUPER_ADMIN', 1);
-- Insert Dummy Venues
INSERT INTO venues (name, type, address, capacity, hourly_price, daily_price, status) VALUES 
('Grand Crystal Mahal', 'Marriage Hall', '123 Main Road, Chennai', 1000, 5000, 50000, 'AVAILABLE'),
('TechHub Boardroom', 'Meeting Room', 'Tidel Park, Chennai', 20, 1000, 8000, 'AVAILABLE'),
('Sunset Outdoor Arena', 'Outdoor Venue', 'ECR Beach, Chennai', 500, 3000, 35000, 'AVAILABLE'),
('Royal Conference Hall', 'Conference Hall', 'Anna Salai, Chennai', 200, 2000, 15000, 'AVAILABLE'),
('Elite Party Lounge', 'Party Hall', 'Nungambakkam, Chennai', 100, 1500, 12000, 'AVAILABLE');

-- Insert Dummy Customers
INSERT INTO customers (name, phone, email, address) VALUES 
('Ramesh Kumar', '9876543210', 'ramesh@example.com', 'Tambaram, Chennai'),
('Priya Sharma', '8765432109', 'priya@example.com', 'Velachery, Chennai'),
('Abdul Rahman', '7654321098', 'abdul@example.com', 'Adyar, Chennai'),
('Anita Patel', '6543210987', 'anita@example.com', 'T Nagar, Chennai'),
('John Doe', '5432109876', 'john@example.com', 'Guindy, Chennai');


-- Insert 10 Dummy Venues
INSERT INTO venues (name, type, address, capacity, hourly_price, daily_price, status) VALUES 
('Grand Crystal Mahal', 'Marriage Hall', 'Chennai', 1000, 5000, 50000, 'AVAILABLE'),
('TechHub Boardroom', 'Meeting Room', 'Chennai', 20, 1000, 8000, 'AVAILABLE'),
('Sunset Outdoor Arena', 'Outdoor Venue', 'ECR Beach', 500, 3000, 35000, 'AVAILABLE'),
('Royal Conference Hall', 'Conference Hall', 'Anna Salai', 200, 2000, 15000, 'AVAILABLE'),
('Elite Party Lounge', 'Party Hall', 'Nungambakkam', 100, 1500, 12000, 'AVAILABLE'),
('Silver Bells Hall', 'Marriage Hall', 'Madurai', 800, 4500, 45000, 'AVAILABLE'),
('Innovate Workspace', 'Meeting Room', 'Coimbatore', 15, 800, 5000, 'AVAILABLE'),
('Skyline Rooftop', 'Party Hall', 'Trichy', 150, 2500, 18000, 'AVAILABLE'),
('Heritage Courtyard', 'Outdoor Venue', 'Tanjore', 300, 2000, 20000, 'AVAILABLE'),
('Apex Auditorium', 'Auditorium', 'Salem', 1500, 8000, 80000, 'AVAILABLE');

-- Insert Dummy Customers
INSERT INTO customers (name, phone, email, address) VALUES 
('Ramesh', '9876543210', 'ramesh@example.com', 'Chennai'),
('Priya', '8765432109', 'priya@example.com', 'Madurai');

-- Insert Dummy Bookings (To show Fixed and Free slots)
-- Grand Crystal Mahal is booked Morning and Night (Leaving Afternoon as a 'Smart Slot Gap')
INSERT INTO bookings (customer_id, venue_id, event_date, start_time, end_time, status) VALUES 
(1, 1, '2026-11-20', '08:00', '13:00', 'CONFIRMED'),
(2, 1, '2026-11-20', '18:00', '23:00', 'CONFIRMED'),
(1, 2, '2026-11-21', '10:00', '12:00', 'CONFIRMED'),
(2, 3, '2026-11-25', '17:00', '22:00', 'PENDING');
-- Clean old data
DELETE FROM bookings;
DELETE FROM venues;
DELETE FROM customers;

-- 5 Neat Venues
INSERT INTO venues (id, name, type, address, capacity, hourly_price, daily_price, status) VALUES 
(1, 'Grand Crystal Mahal', 'Marriage Hall', 'Chennai', 1000, 5000, 50000, 'AVAILABLE'),
(2, 'TechHub Boardroom', 'Meeting Room', 'Chennai', 20, 1000, 8000, 'AVAILABLE'),
(3, 'Sunset Outdoor Arena', 'Outdoor Venue', 'ECR Beach', 500, 3000, 35000, 'AVAILABLE'),
(4, 'Royal Conference', 'Conference Hall', 'Anna Salai', 200, 2000, 15000, 'AVAILABLE'),
(5, 'Elite Party Lounge', 'Party Hall', 'Nungambakkam', 100, 1500, 12000, 'AVAILABLE');

-- 3 Customers
INSERT INTO customers (id, name, phone, email, address) VALUES 
(1, 'Ramesh', '9876543210', 'ramesh@example.com', 'Chennai'),
(2, 'Priya', '8765432109', 'priya@example.com', 'Madurai'),
(3, 'Karthik', '7654321098', 'karthik@example.com', 'Coimbatore');