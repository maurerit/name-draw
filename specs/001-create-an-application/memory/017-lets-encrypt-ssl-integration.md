# Decision Record 017: Let's Encrypt SSL/TLS Integration

## User Prompt
"I was just reading through the research.md and started thinking that we need to take Lets Encrypt into play. I want to get let's encrypt called out and configured within docker/compose somehow..."

## Agent Response
Added comprehensive Let's Encrypt integration to the implementation plan to ensure proper SSL/TLS certificates for production OAuth requirements:

**Research Updates**:
1. **SSL/TLS Certificates Decision**: Added Let's Encrypt as the chosen solution for automated certificate management
2. **Implementation Strategy**: nginx-proxy + acme-companion containers for automatic certificate generation and renewal
3. **Transport Security**: Added HTTPS-only policy with security headers (HSTS, CSP, X-Frame-Options)

**Quickstart Updates**:
1. **Production Deployment Section**: Added complete docker-compose.prod.yml with Let's Encrypt configuration
2. **Domain Configuration**: DNS setup verification steps
3. **SSL Verification**: Certificate validation and testing procedures
4. **Environment Checklist**: Updated to include SSL/TLS requirements

**API Contract Updates**:
1. **Server URLs**: Clarified HTTPS requirement for production/staging
2. **Development Exception**: HTTP allowed only for localhost development

**Technical Implementation**:
- **nginx-proxy**: Reverse proxy with automatic virtual host configuration
- **nginx-proxy-acme**: ACME client for Let's Encrypt certificate automation
- **Certificate Renewal**: Automatic renewal every 60-90 days
- **Multi-domain Support**: Handles both www and non-www domains
- **Security Headers**: HSTS, CSP, and other security headers configured

**OAuth Requirement Satisfaction**:
- Facebook and Google OAuth require HTTPS for production redirect URIs
- Let's Encrypt provides trusted certificates for all major browsers
- Automatic renewal prevents OAuth breakage due to expired certificates

**Container Architecture**:
```
[Internet] → [nginx-proxy:443] → [backend:8080] (api.domain.com)
           → [nginx-proxy:80]  → [frontend:80]  (domain.com)
                                 ↑
                          [nginx-proxy-acme] (certificate management)
```

This ensures secure, production-ready deployment with minimal operational overhead.

## Files Modified
- `/specs/001-create-an-application/research.md`: Added Let's Encrypt decision and transport security section
- `/specs/001-create-an-application/quickstart.md`: Added production SSL setup with docker-compose configuration
- `/specs/001-create-an-application/contracts/api.yaml`: Updated server URLs to clarify HTTPS requirements
