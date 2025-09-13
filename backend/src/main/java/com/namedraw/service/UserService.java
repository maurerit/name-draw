package com.namedraw.service;

import com.namedraw.model.User;
import com.namedraw.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for User entity operations.
 *
 * <p>This service provides business logic for user management including OAuth profile handling,
 * user lifecycle management, and user retrieval operations. All operations respect the isActive
 * flag to prevent access to deactivated accounts.
 *
 * <p>Key responsibilities: - User authentication and profile management - OAuth provider
 * integration support - User account lifecycle (activate/deactivate) - User lookup operations for
 * controllers - Last login tracking
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  /**
   * Finds a user by their ID, only if they are active.
   *
   * @param id the user ID to search for
   * @return Optional containing the user if found and active, empty otherwise
   */
  public Optional<User> findById(UUID id) {
    log.debug("Finding user by id: {}", id);
    Optional<User> user = userRepository.findById(id);
    return user.filter(u -> Boolean.TRUE.equals(u.getIsActive()));
  }

  /**
   * Finds a user by OAuth provider and OAuth ID, only if they are active.
   *
   * @param oauthProvider the OAuth provider (facebook, google)
   * @param oauthId the OAuth provider's user ID
   * @return Optional containing the user if found and active, empty otherwise
   */
  public Optional<User> findByOauthCredentials(String oauthProvider, String oauthId) {
    log.debug("Finding user by OAuth provider: {} and OAuth ID: {}", oauthProvider, oauthId);
    return userRepository.findByOauthProviderAndOauthIdAndIsActiveTrue(oauthProvider, oauthId);
  }

  /**
   * Finds a user by email, only if they are active.
   *
   * @param email the email to search for
   * @return Optional containing the user if found and active, empty otherwise
   */
  public Optional<User> findByEmail(String email) {
    log.debug("Finding user by email: {}", email);
    return userRepository.findByEmailAndIsActiveTrue(email);
  }

  /**
   * Creates a new user with OAuth provider information.
   *
   * @param oauthProvider the OAuth provider (facebook, google)
   * @param oauthId the OAuth provider's user ID
   * @param name the user's display name
   * @param email the user's email (can be null)
   * @param profilePictureUrl the user's profile picture URL (can be null)
   * @return the created user
   * @throws IllegalArgumentException if OAuth provider/ID combination already exists
   */
  @Transactional
  public User createUser(
      String oauthProvider, String oauthId, String name, String email, String profilePictureUrl) {
    log.debug("Creating user for OAuth provider: {} with OAuth ID: {}", oauthProvider, oauthId);

    // Check if user already exists
    if (userRepository.existsByOauthProviderAndOauthId(oauthProvider, oauthId)) {
      throw new IllegalArgumentException(
          String.format(
              "User already exists for OAuth provider %s with ID %s", oauthProvider, oauthId));
    }

    User user =
        User.builder()
            .oauthProvider(oauthProvider)
            .oauthId(oauthId)
            .name(name)
            .email(email)
            .profilePictureUrl(profilePictureUrl)
            .isActive(true)
            .build();

    User savedUser = userRepository.save(user);
    log.info(
        "Created new user with ID: {} for OAuth provider: {}", savedUser.getId(), oauthProvider);
    return savedUser;
  }

  /**
   * Creates or updates a user based on OAuth provider information. If the user exists, updates
   * their profile information and marks them as active. If the user doesn't exist, creates a new
   * user.
   *
   * @param oauthProvider the OAuth provider (facebook, google)
   * @param oauthId the OAuth provider's user ID
   * @param name the user's display name
   * @param email the user's email (can be null)
   * @param profilePictureUrl the user's profile picture URL (can be null)
   * @return the created or updated user
   */
  @Transactional
  public User createOrUpdateUser(
      String oauthProvider, String oauthId, String name, String email, String profilePictureUrl) {
    log.debug(
        "Creating or updating user for OAuth provider: {} with OAuth ID: {}",
        oauthProvider,
        oauthId);

    Optional<User> existingUser =
        userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);

    if (existingUser.isPresent()) {
      User user = existingUser.get();
      user.setName(name);
      user.setEmail(email);
      user.setProfilePictureUrl(profilePictureUrl);
      user.activate();
      user.updateLastLogin();

      User savedUser = userRepository.save(user);
      log.info(
          "Updated existing user with ID: {} for OAuth provider: {}",
          savedUser.getId(),
          oauthProvider);
      return savedUser;
    } else {
      return createUser(oauthProvider, oauthId, name, email, profilePictureUrl);
    }
  }

  /**
   * Updates the last login timestamp for a user.
   *
   * @param userId the ID of the user to update
   * @return true if the user was found and updated, false otherwise
   */
  @Transactional
  public boolean updateLastLogin(UUID userId) {
    log.debug("Updating last login for user ID: {}", userId);

    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isPresent() && Boolean.TRUE.equals(userOpt.get().getIsActive())) {
      User user = userOpt.get();
      user.updateLastLogin();
      userRepository.save(user);
      log.debug("Updated last login for user ID: {}", userId);
      return true;
    }

    log.warn("Could not find active user with ID: {} to update last login", userId);
    return false;
  }

  /**
   * Deactivates a user account.
   *
   * @param userId the ID of the user to deactivate
   * @return true if the user was found and deactivated, false otherwise
   */
  @Transactional
  public boolean deactivateUser(UUID userId) {
    log.debug("Deactivating user with ID: {}", userId);

    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      user.deactivate();
      userRepository.save(user);
      log.info("Deactivated user with ID: {}", userId);
      return true;
    }

    log.warn("Could not find user with ID: {} to deactivate", userId);
    return false;
  }

  /**
   * Activates a user account.
   *
   * @param userId the ID of the user to activate
   * @return true if the user was found and activated, false otherwise
   */
  @Transactional
  public boolean activateUser(UUID userId) {
    log.debug("Activating user with ID: {}", userId);

    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      user.activate();
      userRepository.save(user);
      log.info("Activated user with ID: {}", userId);
      return true;
    }

    log.warn("Could not find user with ID: {} to activate", userId);
    return false;
  }

  /**
   * Gets all active users.
   *
   * @return list of all active users
   */
  public List<User> findAllActive() {
    log.debug("Finding all active users");
    return userRepository.findByIsActiveTrue();
  }

  /**
   * Gets users who haven't logged in since the specified date.
   *
   * @param since the date threshold for last login
   * @return list of users who haven't logged in since the specified date
   */
  public List<User> findUsersNotLoggedInSince(LocalDateTime since) {
    log.debug("Finding users not logged in since: {}", since);
    return userRepository.findByIsActiveTrue().stream()
        .filter(user -> user.getLastLogin() == null || user.getLastLogin().isBefore(since))
        .toList();
  }

  /**
   * Gets users by OAuth provider.
   *
   * @param oauthProvider the OAuth provider to filter by
   * @return list of active users for the specified OAuth provider
   */
  public List<User> findByOauthProvider(String oauthProvider) {
    log.debug("Finding users by OAuth provider: {}", oauthProvider);
    return userRepository.findByOauthProviderAndIsActiveTrue(oauthProvider);
  }

  /**
   * Checks if a user exists and is active.
   *
   * @param userId the user ID to check
   * @return true if the user exists and is active, false otherwise
   */
  public boolean existsAndIsActive(UUID userId) {
    log.debug("Checking if user exists and is active: {}", userId);
    Optional<User> user = userRepository.findById(userId);
    return user.isPresent() && Boolean.TRUE.equals(user.get().getIsActive());
  }

  /**
   * Gets the total count of active users.
   *
   * @return the number of active users
   */
  public long getActiveUserCount() {
    log.debug("Getting active user count");
    return userRepository.countByIsActiveTrue();
  }
}
