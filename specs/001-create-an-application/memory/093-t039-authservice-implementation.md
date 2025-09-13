# T039 Implementation - AuthService OAuth Integration

**Request**: Implement T039 - AuthService OAuth integration (depends on UserService) in `backend/src/main/java/com/namedraw/service/AuthService.java`

**User Input**: "Alright claude, familiarize yourself with our constitution the spec in specs/001-create-an-application and then implement T039. Don't forget to log our interaction and your decisions"

## Constitution Analysis
- **PRINCIPLE_2_NAME (Test-First)**: TDD mandatory - Contract tests T009-T012 already exist and must pass
- **PRINCIPLE_5_NAME (Boilerplate go away)**: Use Lombok to minimize boilerplate in all classes  
- **PRINCIPLE_6_NAME (We have checks for a reason)**: Do not skip code style checks during compilation
- **SECTION_1_NAME**: Login with Google and Facebook OAuth required for family members

## Spec Requirements Analysis
- **FR-001**: System MUST allow users to authenticate via Facebook OAuth
- **FR-002**: System MUST allow users to authenticate via Google OAuth
- **FR-023**: System MUST validate all participant social accounts before draw transitions
- **FR-024**: System MUST automatically remove participants with deactivated social accounts

## API Contract Analysis (from contracts/api.yaml)
- **GET /auth/login/{provider}**: Redirects to OAuth provider (302 response)
- **POST /auth/callback/{provider}**: Processes OAuth callback, returns AuthResponse with JWT
- **POST /auth/refresh**: Refreshes JWT token using refresh token
- **POST /auth/logout**: Invalidates JWT token

## Implementation Decisions
1. **OAuth Integration**: Use Spring Security OAuth2 client with existing configuration in application.yml
2. **JWT Token Management**: Create JWT service for token generation, validation, refresh, and invalidation
3. **User Management**: Integrate with existing UserService for user creation/retrieval by OAuth details
4. **Error Handling**: Return appropriate HTTP status codes as defined in contract (400, 401)
5. **Security**: Implement CSRF protection with state parameter validation
6. **Provider Support**: Support both Google and Facebook OAuth providers with different user attribute mappings

## Dependencies Identified
- UserService (already implemented) - for user creation and OAuth lookup
- JWT token service (needs implementation)
- Spring Security OAuth2 client configuration (already configured)
- OAuth provider user info parsing

**Final Response**: Proceeding with AuthService implementation following TDD approach, respecting constitution principles, and implementing all required OAuth flows.