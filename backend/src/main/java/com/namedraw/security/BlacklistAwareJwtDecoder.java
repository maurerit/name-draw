package com.namedraw.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * JWT decoder that wraps another decoder and adds blacklist checking.
 *
 * <p>This decoder checks if the token is blacklisted first, and if not, validates tokens using the
 * wrapped decoder. If the token is blacklisted, a BadJwtException is thrown to trigger proper
 * authentication failure handling.
 */
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {

  private final JwtDecoder delegate;
  private final JwtTokenService jwtTokenService;

  @Override
  public Jwt decode(String token) throws JwtException {
    // First check if the token is blacklisted
    if (jwtTokenService.isTokenBlacklisted(token)) {
      throw new BadJwtException("JWT token has been invalidated");
    }

    // Then decode the token using the wrapped decoder
    return delegate.decode(token);
  }
}
