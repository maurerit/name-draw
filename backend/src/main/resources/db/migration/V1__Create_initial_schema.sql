-- Create Users table
CREATE TABLE users (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    oauth_provider VARCHAR(20) NOT NULL CHECK (oauth_provider IN ('facebook', 'google')),
    oauth_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL CHECK (LENGTH(TRIM(name)) > 0),
    email VARCHAR(255),
    profile_picture_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    last_login TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Composite unique constraint for OAuth provider + ID
    CONSTRAINT uk_users_oauth UNIQUE (oauth_provider, oauth_id)
);

-- Create index for OAuth lookup
CREATE INDEX idx_users_oauth_provider ON users(oauth_provider);

-- Create Draws table  
CREATE TABLE draws (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    creator_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL CHECK (LENGTH(TRIM(title)) > 0),
    description TEXT,
    state VARCHAR(20) NOT NULL DEFAULT 'JOINING' CHECK (state IN ('JOINING', 'OPEN', 'ARCHIVED')),
    draw_date DATE NOT NULL,
    participant_count INTEGER NOT NULL DEFAULT 0 CHECK (participant_count >= 0),
    max_participants INTEGER NOT NULL DEFAULT 30 CHECK (max_participants > 0 AND max_participants <= 30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    opened_at TIMESTAMP,
    archived_at TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_draws_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Business rule constraints
    CONSTRAINT chk_draws_participant_limit CHECK (participant_count <= max_participants),
    CONSTRAINT chk_draws_date_future CHECK (draw_date >= CURRENT_DATE())
);

-- Create indexes for efficient queries
CREATE INDEX idx_draws_creator_id ON draws(creator_id);
CREATE INDEX idx_draws_state ON draws(state);
CREATE INDEX idx_draws_draw_date ON draws(draw_date);

-- Create Participations table
CREATE TABLE participations (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL,
    draw_id UUID NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    
    -- Foreign key constraints
    CONSTRAINT fk_participations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_participations_draw FOREIGN KEY (draw_id) REFERENCES draws(id) ON DELETE CASCADE,
    
    -- Unique constraint - user can only join a draw once
    CONSTRAINT uk_participations_user_draw UNIQUE (user_id, draw_id)
);

-- Create indexes for foreign key lookups
CREATE INDEX idx_participations_user_id ON participations(user_id);
CREATE INDEX idx_participations_draw_id ON participations(draw_id);

-- Create DrawnNames table
CREATE TABLE drawn_names (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    draw_id UUID NOT NULL,
    drawer_user_id UUID NOT NULL,
    drawn_user_id UUID NOT NULL,
    drawn_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    
    -- Foreign key constraints
    CONSTRAINT fk_drawn_names_draw FOREIGN KEY (draw_id) REFERENCES draws(id) ON DELETE CASCADE,
    CONSTRAINT fk_drawn_names_drawer FOREIGN KEY (drawer_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_drawn_names_drawn FOREIGN KEY (drawn_user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Business rule constraints
    CONSTRAINT uk_drawn_names_drawer_draw UNIQUE (draw_id, drawer_user_id),
    CONSTRAINT uk_drawn_names_drawn_draw UNIQUE (draw_id, drawn_user_id),
    CONSTRAINT chk_drawn_names_no_self_draw CHECK (drawer_user_id != drawn_user_id)
);

-- Create indexes for efficient queries
CREATE INDEX idx_drawn_names_draw_id ON drawn_names(draw_id);
CREATE INDEX idx_drawn_names_drawer_user_id ON drawn_names(drawer_user_id);
CREATE INDEX idx_drawn_names_drawn_user_id ON drawn_names(drawn_user_id);

-- Create DrawQueue table (in-memory for concurrency control)
CREATE MEMORY TABLE draw_queue (
    draw_id UUID PRIMARY KEY,
    
    -- Foreign key constraint
    CONSTRAINT fk_draw_queue_draw FOREIGN KEY (draw_id) REFERENCES draws(id) ON DELETE CASCADE
);

-- Create JoinQueue table (in-memory for join concurrency control)
CREATE MEMORY TABLE join_queue (
    draw_id UUID PRIMARY KEY,

    -- Foreign key constraint
    CONSTRAINT fk_join_queue_draw FOREIGN KEY (draw_id) REFERENCES draws(id) ON DELETE CASCADE
);
