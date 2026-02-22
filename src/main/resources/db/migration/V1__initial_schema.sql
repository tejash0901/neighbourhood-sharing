-- Flyway Migration V1: Initial Schema
-- NeighborhoodShare Platform - Core Tables

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============ APARTMENTS ============

CREATE TABLE apartments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    invite_code VARCHAR(50) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) DEFAULT 'USA',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL
);

CREATE INDEX idx_apartments_invite_code ON apartments(invite_code);
CREATE INDEX idx_apartments_city ON apartments(city);

-- ============ USERS ============

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    apartment_id UUID NOT NULL REFERENCES apartments(id) ON DELETE CASCADE,

    -- Profile
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    profile_pic_url TEXT,
    bio TEXT,

    -- Authentication
    password_hash VARCHAR(255),
    otp_secret VARCHAR(100),
    verified_at TIMESTAMP,

    -- Rating & Trust
    average_rating DECIMAL(3,2) DEFAULT 0,
    total_ratings INT DEFAULT 0,
    total_borrowed_items INT DEFAULT 0,
    total_lent_items INT DEFAULT 0,
    total_items_returned_on_time INT DEFAULT 0,
    trust_score INT DEFAULT 50,

    -- Wallet
    wallet_balance DECIMAL(10,2) DEFAULT 0,

    -- Status
    is_active BOOLEAN DEFAULT true,
    banned_until TIMESTAMP,
    ban_reason TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,

    CONSTRAINT valid_rating CHECK (average_rating >= 0 AND average_rating <= 5),
    CONSTRAINT valid_trust_score CHECK (trust_score >= 0 AND trust_score <= 100)
);

CREATE INDEX idx_users_apartment_id ON users(apartment_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);

-- ============ ITEMS ============

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    apartment_id UUID NOT NULL REFERENCES apartments(id) ON DELETE CASCADE,

    -- Basic Info
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,

    -- Pricing
    price_per_hour DECIMAL(10,2) NOT NULL,
    price_per_day DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Availability
    is_available BOOLEAN DEFAULT true,
    max_consecutive_days INT DEFAULT 7,

    -- Images (JSON array)
    images JSONB DEFAULT '[]'::jsonb,

    -- Condition
    current_condition VARCHAR(50) DEFAULT 'good',
    damage_notes TEXT,

    -- Rating
    average_rating DECIMAL(3,2) DEFAULT 0,
    total_bookings INT DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP

);

CREATE INDEX idx_items_owner_id ON items(owner_id);
CREATE INDEX idx_items_apartment_id ON items(apartment_id);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_items_available ON items(is_available);
CREATE INDEX idx_items_deleted_at ON items(deleted_at);

-- ============ BOOKINGS ============

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    borrower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Booking Details
    status VARCHAR(50) NOT NULL DEFAULT 'requested',
    status_updated_at TIMESTAMP,

    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,

    duration_days INT GENERATED ALWAYS AS (EXTRACT(DAY FROM (end_date - start_date))::INT + 1) STORED,

    -- Pricing
    base_price DECIMAL(10,2) NOT NULL,
    deposit_collected DECIMAL(10,2) DEFAULT 0,
    platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    paid_at TIMESTAMP,
    payment_intent_id VARCHAR(255),

    -- Return Info
    returned_at TIMESTAMP,
    damage_reported BOOLEAN DEFAULT false,
    damage_amount DECIMAL(10,2) DEFAULT 0,
    return_notes TEXT,
    return_images JSONB DEFAULT '[]'::jsonb,

    -- Rating
    borrower_rating_given BOOLEAN DEFAULT false,
    borrower_rating DECIMAL(3,2),
    borrower_review TEXT,
    owner_rating_given BOOLEAN DEFAULT false,
    owner_rating DECIMAL(3,2),
    owner_review TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_item_id ON bookings(item_id);
CREATE INDEX idx_bookings_borrower_id ON bookings(borrower_id);
CREATE INDEX idx_bookings_owner_id ON bookings(owner_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_dates ON bookings(start_date, end_date);

-- ============ AVAILABILITY BLOCKS ============

CREATE TABLE availability_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    block_type VARCHAR(50) NOT NULL,

    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_availability_item_id ON availability_blocks(item_id);
CREATE INDEX idx_availability_dates ON availability_blocks(start_date, end_date);

-- ============ TRANSACTIONS ============

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_id UUID REFERENCES bookings(id) ON DELETE SET NULL,

    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',

    stripe_transaction_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'pending',

    description TEXT,
    metadata JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_booking_id ON transactions(booking_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- ============ REVIEWS ============

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,

    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewed_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,

    rating DECIMAL(3,2) NOT NULL,
    title VARCHAR(255),
    content TEXT,

    helpful_count INT DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_review_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_reviews_booking_id ON reviews(booking_id);
CREATE INDEX idx_reviews_reviewer_id ON reviews(reviewer_id);
CREATE INDEX idx_reviews_reviewed_user_id ON reviews(reviewed_user_id);
CREATE INDEX idx_reviews_item_id ON reviews(item_id);

-- ============ DISPUTES ============

CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,

    created_by_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dispute_reason VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    evidence JSONB DEFAULT '[]'::jsonb,

    status VARCHAR(50) DEFAULT 'open',
    assigned_admin_id UUID REFERENCES users(id),

    resolution TEXT,
    refund_amount DECIMAL(10,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_disputes_booking_id ON disputes(booking_id);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_created_by_id ON disputes(created_by_id);

-- ============ ADMIN LOGS ============

CREATE TABLE admin_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID REFERENCES users(id),

    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,

    old_values JSONB,
    new_values JSONB,

    ip_address INET,
    user_agent TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_logs_admin_id ON admin_logs(admin_id);
CREATE INDEX idx_admin_logs_created_at ON admin_logs(created_at);

-- ============ FRAUD SCORES ============

CREATE TABLE fraud_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    score INT NOT NULL,
    reason TEXT,
    flagged_behavior JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_scores_user_id ON fraud_scores(user_id);
CREATE INDEX idx_fraud_scores_score ON fraud_scores(score);
