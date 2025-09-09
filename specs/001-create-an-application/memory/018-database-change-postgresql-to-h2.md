# Decision Record 018: Database Change from PostgreSQL to H2

## User Prompt
"You chose PostgreSQL I see but I want to change that decision. It's easier for me and for this application (30 people is WAY OPTIMISTIC) so I want to use H2. We'll still persist data and still interact with a server but this server is so much liter than postgre or mysql"

## Agent Response
Changed the database decision from PostgreSQL to H2 database to better match the application's scale and simplicity requirements:

**Database Decision Change**:
- **FROM**: PostgreSQL for primary data storage
- **TO**: H2 embedded database for primary data storage

**Rationale for H2**:
1. **Perfect Scale Match**: Family-oriented application with max 30 participants per draw
2. **Simplicity**: Zero-configuration embedded mode, no separate database server needed
3. **Persistence**: Server mode provides persistence across application restarts
4. **Development Friendly**: Built-in web console at `/h2-console` for debugging
5. **Lightweight**: Minimal resource footprint ideal for containerized deployment
6. **Fast Performance**: Excellent performance for small to medium datasets
7. **Spring Boot Integration**: Excellent integration with minimal configuration

**Updated Implementation Details**:
- **Database URL**: `jdbc:h2:file:./data/namedraw_dev;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`
- **Web Console**: Available at `http://localhost:8080/h2-console` for development
- **File Storage**: Database persisted to `./data/namedraw_dev.mv.db`
- **Testing**: H2 in-memory mode for fast unit and integration tests
- **Production**: H2 server mode with Docker volume for data persistence

**Documentation Updates**:
1. **research.md**: Updated database decision rationale and testing strategy
2. **quickstart.md**: Simplified setup (no PostgreSQL installation), updated Docker configs
3. **data-model.md**: Added H2-specific considerations and features
4. **docker-compose.prod.yml**: Removed PostgreSQL service, simplified to H2 with volume

**Benefits Realized**:
- Eliminated need for separate database container in production
- Simplified development setup (no PostgreSQL installation required)
- Reduced memory footprint and deployment complexity
- Maintained ACID compliance and data integrity
- Kept all existing JPA/Hibernate functionality

**Alternatives Reconsidered**:
- PostgreSQL: Rejected as overkill for family-scale application
- MySQL: Rejected for unnecessary complexity overhead  
- SQLite: H2 chosen for better Java integration and server mode

This change significantly simplifies the application architecture while maintaining all required functionality for the target use case.

## Files Modified
- `/specs/001-create-an-application/research.md`: Updated database decision and testing strategy
- `/specs/001-create-an-application/quickstart.md`: Simplified setup, removed PostgreSQL references, updated Docker configs
- `/specs/001-create-an-application/data-model.md`: Added H2-specific database considerations
