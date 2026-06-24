-- Enable PostGIS for advanced geospatial types and indexing
CREATE EXTENSION IF NOT EXISTS postgis;

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: Restaurants and Menu Ecosystem
CREATE TABLE IF NOT EXISTS restaurants (
    restaurant_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address_text TEXT NOT NULL,
    -- Location stored as a PostGIS point geometry
    location GEOMETRY(Point, 4326) NOT NULL,
    preparation_time_minutes INTEGER DEFAULT 15,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Crucial: GiST Index for high-speed spatial bounding box queries
CREATE INDEX IF NOT EXISTS idx_restaurants_location ON restaurants USING GIST (location);

-- Table: Customer Identities
CREATE TABLE IF NOT EXISTS customers (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    default_address TEXT,
    default_location GEOMETRY(Point, 4326),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_customers_location ON customers USING GIST (default_location);

-- Table: Driver Profiles (Static data, real-time data lives in Redis)
CREATE TABLE IF NOT EXISTS drivers (
    driver_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL, -- driving, bike, walking
    status VARCHAR(50) DEFAULT 'OFFLINE', 
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: Order Management
CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(customer_id),
    restaurant_id UUID REFERENCES restaurants(restaurant_id),
    driver_id UUID REFERENCES drivers(driver_id),
    pickup_location GEOMETRY(Point, 4326) NOT NULL,
    dropoff_location GEOMETRY(Point, 4326) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ASSIGNED, PICKED_UP, DELIVERED
    fare_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: Historical Telemetry Traces
CREATE TABLE IF NOT EXISTS driver_traces_history (
    trace_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID REFERENCES drivers(driver_id),
    path GEOMETRY(LineString, 4326) NOT NULL,
    recorded_at TIMESTAMPTZ DEFAULT NOW()
);
