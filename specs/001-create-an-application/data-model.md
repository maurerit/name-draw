# Data Model: Name Draw Application

**Date**: September 8, 2025  
**Feature**: Name Draw Application with Social Login  
**Source**: Extracted from feature specification functional requirements

## Entity Definitions

### User
**Purpose**: Represents authenticated individuals via OAuth social login

**Fields**:
- `id` (UUID, Primary Key): Unique identifier for the user
- `oauth_provider` (String, Not Null): OAuth provider (facebook, google)
- `oauth_id` (String, Not Null): Unique ID from OAuth provider
- `name` (String, Not Null): Display name from OAuth profile
- `email` (String, Nullable): Email from OAuth profile (if available)
- `profile_picture_url` (String, Nullable): Profile image URL from OAuth
- `created_at` (Timestamp, Not Null): Account creation timestamp
- `last_login` (Timestamp, Nullable): Last login timestamp
- `is_active` (Boolean, Not Null, Default: true): Account status

**Validation Rules**:
- `oauth_provider` must be one of: facebook, google
- `oauth_id` must be unique per provider
- `name` cannot be empty or whitespace only
- Combination of `oauth_provider` + `oauth_id` must be unique

**Relationships**:
- One-to-Many with Draw (as creator)
- One-to-Many with Participation (as participant)

### Draw
**Purpose**: Represents a name drawing event with lifecycle states

**Fields**:
- `id` (UUID, Primary Key): Unique identifier for the draw
- `creator_id` (UUID, Foreign Key to User, Not Null): User who created the draw
- `title` (String, Not Null): Display name for the draw
- `description` (String, Nullable): Optional description
- `state` (Enum, Not Null): Current state (JOINING, OPEN, ARCHIVED)
- `draw_date` (Date, Not Null): Date when draw should be archived
- `participant_count` (Integer, Not Null, Default: 0): Current number of participants
- `max_participants` (Integer, Not Null, Default: 30): Maximum allowed participants
- `created_at` (Timestamp, Not Null): Draw creation timestamp
- `opened_at` (Timestamp, Nullable): When draw was opened for drawing
- `archived_at` (Timestamp, Nullable): When draw was archived

**Validation Rules**:
- `state` must be one of: JOINING, OPEN, ARCHIVED
- `draw_date` cannot be in the past when creating
- `participant_count` cannot exceed `max_participants`
- `max_participants` cannot exceed 30
- `title` cannot be empty or whitespace only

**State Transitions**:
- JOINING → OPEN: Manual transition by creator or automatic at 30 participants
- OPEN → ARCHIVED: Automatic transition on draw_date
- JOINING → ARCHIVED: Automatic transition on draw_date (if only 1 participant)

**Relationships**:
- Many-to-One with User (creator)
- One-to-Many with Participation
- One-to-Many with DrawnName
- One-to-One with DrawQueue (optional)

### Participation
**Purpose**: Links users to draws they've joined

**Fields**:
- `id` (UUID, Primary Key): Unique identifier for the participation
- `user_id` (UUID, Foreign Key to User, Not Null): Participating user
- `draw_id` (UUID, Foreign Key to Draw, Not Null): Draw being joined
- `joined_at` (Timestamp, Not Null): When user joined the draw

**Validation Rules**:
- Combination of `user_id` + `draw_id` must be unique (user can't join same draw twice)
- Cannot create participation for draws in OPEN or ARCHIVED state
- Cannot create participation if draw is at max capacity

**Relationships**:
- Many-to-One with User
- Many-to-One with Draw

### DrawnName
**Purpose**: Records which participant drew which name

**Fields**:
- `id` (UUID, Primary Key): Unique identifier for the drawn name record
- `draw_id` (UUID, Foreign Key to Draw, Not Null): Draw this result belongs to
- `drawer_user_id` (UUID, Foreign Key to User, Not Null): User who performed the draw
- `drawn_user_id` (UUID, Foreign Key to User, Not Null): User whose name was drawn
- `drawn_at` (Timestamp, Not Null): When the draw was performed

**Validation Rules**:
- Combination of `draw_id` + `drawer_user_id` must be unique (one draw per user per draw)
- Combination of `draw_id` + `drawn_user_id` must be unique (each name drawn only once)
- `drawer_user_id` must be a participant in the draw
- `drawn_user_id` must be a participant in the draw
- `drawer_user_id` and `drawn_user_id` must be different (no self-draws in final result)
- Can only create for draws in OPEN state

**Relationships**:
- Many-to-One with Draw
- Many-to-One with User (drawer)
- Many-to-One with User (drawn)

### DrawQueue
**Purpose**: Tracks participants eligible to draw names (in-memory for performance)

**Fields**:
- `draw_id` (UUID, Primary Key): Draw identifier (only field in table)

**Storage**: In-memory table for high-performance transactional operations

**Purpose**: 
- Atomic operations for concurrent draw management
- Insert record when participant attempts to draw
- Delete after the draw operation is complete
- Primary key constraint ensures only one person can draw at a time
- Minimal memory footprint (single UUID per active draw operation)

**Lifecycle**:
- Record inserted when user clicks draw button
- Record deleted when draw operation completes (success or failure)
- Table remains empty when no draws are in progress

**Validation Rules**:
- `draw_id` must exist in Draw table
- `draw_id` must reference a draw in OPEN state
- Only one record per draw_id can exist (primary key constraint)

**Relationships**:
- One-to-One with Draw (while draw operation is in progress)

## Database Schema Considerations (H2 Database)

### H2-Specific Features
- Server mode for persistence across application restarts
- Built-in web console for development (`/h2-console`)
- Automatic schema creation and migration support
- MVCC (Multi-Version Concurrency Control) for better performance
- Compatible with standard SQL and JPA annotations

### Indexes
- `User.oauth_provider + User.oauth_id` (unique composite index)
- `Draw.creator_id` (foreign key index)
- `Draw.state` (query optimization)
- `Draw.draw_date` (for archival operations)
- `Participation.user_id` (foreign key index)
- `Participation.draw_id` (foreign key index)
- `Participation.user_id + Participation.draw_id` (unique composite index)
- `DrawnName.draw_id` (foreign key index)
- `DrawnName.drawer_user_id` (foreign key index)
- `DrawnName.drawn_user_id` (foreign key index)

### Constraints
- Foreign key constraints for all relationships
- Unique constraints for business rules
- Check constraints for enum values and ranges
- Not null constraints for required fields

### Concurrency Control
- Optimistic locking with version fields where needed
- In-memory DrawQueue for atomic draw operations
- Database-level constraints for data integrity

### Data Archival
- Soft delete pattern not needed (draws are archived, not deleted)
- Historical data preserved for audit and user access
- Automatic archival based on draw_date

## API Data Transfer Objects

### UserDTO
```json
{
  "id": "uuid",
  "name": "string",
  "profilePictureUrl": "string",
  "isActive": "boolean"
}
```

### DrawDTO
```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "state": "JOINING|OPEN|ARCHIVED",
  "drawDate": "date",
  "participantCount": "integer",
  "maxParticipants": "integer",
  "creator": "UserDTO",
  "participants": ["UserDTO"],
  "canJoin": "boolean",
  "canDraw": "boolean"
}
```

### DrawResultDTO
```json
{
  "drawId": "uuid",
  "drawnUser": "UserDTO",
  "drawnAt": "timestamp"
}
```

### ParticipationDTO
```json
{
  "id": "uuid",
  "draw": "DrawDTO",
  "joinedAt": "timestamp"
}
```
