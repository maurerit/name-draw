-- Insert sample users for development
INSERT INTO users (id, oauth_provider, oauth_id, name, email, profile_picture_url, created_at, is_active) VALUES
    ('11111111-1111-1111-1111-111111111111', 'google', 'google_123', 'John Doe', 'john.doe@example.com', 'https://example.com/avatar1.jpg', CURRENT_TIMESTAMP(), TRUE),
    ('22222222-2222-2222-2222-222222222222', 'facebook', 'facebook_456', 'Jane Smith', 'jane.smith@example.com', 'https://example.com/avatar2.jpg', CURRENT_TIMESTAMP(), TRUE),
    ('33333333-3333-3333-3333-333333333333', 'google', 'google_789', 'Bob Johnson', 'bob.johnson@example.com', NULL, CURRENT_TIMESTAMP(), TRUE),
    ('44444444-4444-4444-4444-444444444444', 'facebook', 'facebook_321', 'Alice Williams', 'alice.williams@example.com', 'https://example.com/avatar4.jpg', CURRENT_TIMESTAMP(), TRUE),
    ('55555555-5555-5555-5555-555555555555', 'google', 'google_654', 'Charlie Brown', 'charlie.brown@example.com', NULL, CURRENT_TIMESTAMP(), TRUE);

-- Insert sample draw
INSERT INTO draws (id, creator_id, title, description, state, draw_date, participant_count, max_participants, created_at) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Office Secret Santa 2025', 'Annual office holiday gift exchange', 'JOINING', '2025-12-25', 3, 10, CURRENT_TIMESTAMP());

-- Insert sample participations
INSERT INTO participations (id, user_id, draw_id, joined_at) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', CURRENT_TIMESTAMP()),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', CURRENT_TIMESTAMP()),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', CURRENT_TIMESTAMP());

-- Update participant count to match actual participations
UPDATE draws SET participant_count = 3 WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
