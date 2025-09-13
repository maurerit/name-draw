package com.namedraw.client;

import java.util.Map;

/**
 * Client interface for OAuth provider interactions.
 *
 * <p>This interface defines the contract for communicating with OAuth providers (Google, Facebook)
 * to exchange authorization codes for access tokens and retrieve user information. By abstracting
 * the HTTP client interactions, this interface enables easy mocking for unit tests and provides
 * flexibility to change the underlying HTTP client implementation.
 *
 * <p>Implementations should handle: - OAuth token exchange (authorization code to access token) -
 * User profile retrieval from OAuth providers - Proper error handling and response parsing -
 * Authentication headers and request formatting
 */
public interface AuthClient {

  /**
   * Exchanges an authorization code for an access token with the OAuth provider.
   *
   * @param tokenUri the OAuth provider's token endpoint URI
   * @param clientId the OAuth client ID
   * @param clientSecret the OAuth client secret
   * @param code the authorization code received from the provider
   * @param redirectUri the redirect URI used in the OAuth flow
   * @return the access token response as a Map containing token information
   * @throws RuntimeException if the token exchange fails
   */
  Map<String, Object> exchangeCodeForToken(
      String tokenUri, String clientId, String clientSecret, String code, String redirectUri);

  /**
   * Retrieves user information from the OAuth provider using an access token.
   *
   * @param userInfoUri the OAuth provider's user info endpoint URI
   * @param accessToken the access token for authentication
   * @return the user information response as a Map containing user attributes
   * @throws RuntimeException if the user info request fails
   */
  Map<String, Object> getUserInfo(String userInfoUri, String accessToken);
}
