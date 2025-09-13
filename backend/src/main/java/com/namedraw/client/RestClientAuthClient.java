package com.namedraw.client;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * RestClient-based implementation of AuthClient for OAuth provider interactions.
 *
 * <p>This implementation uses Spring's RestClient to communicate with OAuth providers (Google,
 * Facebook) for token exchange and user information retrieval. It handles HTTP request formatting,
 * headers, and response parsing for OAuth operations.
 *
 * <p>Key features: - Exchanges authorization codes for access tokens - Retrieves user profile
 * information using access tokens - Proper HTTP header management for OAuth requests - Robust error
 * handling and logging - Type-safe response parsing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestClientAuthClient implements AuthClient {

  private final RestClient restClient;

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
  @Override
  public Map<String, Object> exchangeCodeForToken(
      String tokenUri, String clientId, String clientSecret, String code, String redirectUri) {
    log.debug("Exchanging authorization code for access token at: {}", tokenUri);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("code", code);
    params.add("grant_type", "authorization_code");
    params.add("redirect_uri", redirectUri);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add("Content-Type", "application/x-www-form-urlencoded");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    try {
      @SuppressWarnings("rawtypes")
      ResponseEntity<Map> response =
          restClient
              .post()
              .uri(tokenUri)
              .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders()))
              .body(request.getBody())
              .retrieve()
              .toEntity(Map.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
      if (responseBody == null || !responseBody.containsKey("access_token")) {
        throw new RuntimeException("Failed to obtain access token from provider");
      }

      log.debug("Successfully exchanged authorization code for access token");
      return responseBody;
    } catch (Exception e) {
      log.error("Error exchanging code for token: {}", e.getMessage());
      throw new RuntimeException("Failed to exchange authorization code for token", e);
    }
  }

  /**
   * Retrieves user information from the OAuth provider using an access token.
   *
   * @param userInfoUri the OAuth provider's user info endpoint URI
   * @param accessToken the access token for authentication
   * @return the user information response as a Map containing user attributes
   * @throws RuntimeException if the user info request fails
   */
  @Override
  public Map<String, Object> getUserInfo(String userInfoUri, String accessToken) {
    log.debug("Retrieving user information from: {}", userInfoUri);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    HttpEntity<String> request = new HttpEntity<>(headers);

    try {
      @SuppressWarnings("rawtypes")
      ResponseEntity<Map> response =
          restClient
              .get()
              .uri(userInfoUri)
              .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders()))
              .retrieve()
              .toEntity(Map.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
      if (responseBody == null) {
        throw new RuntimeException("Failed to get user info from provider");
      }

      log.debug("Successfully retrieved user information");
      return responseBody;
    } catch (Exception e) {
      log.error("Error getting user info: {}", e.getMessage());
      throw new RuntimeException("Failed to get user information from provider", e);
    }
  }
}
