package com.namedraw.repository;

import com.namedraw.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository interface for User entity operations.
 *
 * <p>This repository provides data access methods for User entities with a focus on OAuth
 * authentication scenarios and user lifecycle management. All finder methods respect the isActive
 * flag to prevent access to deactivated accounts.
 *
 * <p>Key functionality includes: - OAuth provider-based user lookup for authentication flows -
 * Active user management and filtering - Profile data access for authenticated sessions - Bulk
 * operations for user administration
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Finds a user by their OAuth provider and provider-specific ID.
   *
   * <p>This is the primary method for OAuth authentication flows. The combination of oauthProvider
   * and oauthId uniquely identifies a user across the system, as enforced by the database unique
   * constraint.
   *
   * @param oauthProvider the OAuth provider (e.g., "facebook", "google")
   * @param oauthId the unique ID from the OAuth provider
   * @return Optional containing the user if found, empty otherwise
   */
  Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

  /**
   * Finds an active user by their OAuth provider and provider-specific ID.
   *
   * <p>Similar to {@link #findByOauthProviderAndOauthId(String, String)} but only returns users
   * with isActive=true. This is the preferred method for authentication flows to prevent login of
   * deactivated accounts.
   *
   * @param oauthProvider the OAuth provider (e.g., "facebook", "google")
   * @param oauthId the unique ID from the OAuth provider
   * @return Optional containing the active user if found, empty otherwise
   */
  Optional<User> findByOauthProviderAndOauthIdAndIsActiveTrue(String oauthProvider, String oauthId);

  /**
   * Finds a user by their email address.
   *
   * <p>Note that email addresses are optional in OAuth flows and may be null for some users. This
   * method is primarily used for administrative purposes or email-based notifications.
   *
   * @param email the user's email address
   * @return Optional containing the user if found, empty otherwise
   */
  Optional<User> findByEmail(String email);

  /**
   * Finds an active user by their email address.
   *
   * <p>Similar to {@link #findByEmail(String)} but only returns users with isActive=true.
   *
   * @param email the user's email address
   * @return Optional containing the active user if found, empty otherwise
   */
  Optional<User> findByEmailAndIsActiveTrue(String email);

  /**
   * Finds all active users in the system.
   *
   * <p>Returns all users where isActive=true. This method should be used carefully as it may return
   * large result sets. Consider using pagination for production use.
   *
   * @return List of all active users
   */
  List<User> findByIsActiveTrue();

  /**
   * Finds all users for a specific OAuth provider.
   *
   * <p>This method is useful for administrative reporting or migration scenarios where you need to
   * analyze users by their authentication provider.
   *
   * @param oauthProvider the OAuth provider (e.g., "facebook", "google")
   * @return List of all users from the specified provider
   */
  List<User> findByOauthProvider(String oauthProvider);

  /**
   * Finds all active users for a specific OAuth provider.
   *
   * <p>Combines provider filtering with active status filtering.
   *
   * @param oauthProvider the OAuth provider (e.g., "facebook", "google")
   * @return List of all active users from the specified provider
   */
  List<User> findByOauthProviderAndIsActiveTrue(String oauthProvider);

  /**
   * Finds users who have logged in since a specific date.
   *
   * <p>Useful for identifying active users over a time period. Users with null lastLogin timestamps
   * are excluded from results.
   *
   * @param since the cutoff date for last login
   * @return List of users who logged in after the specified date
   */
  List<User> findByLastLoginAfter(LocalDateTime since);

  /**
   * Finds active users who have logged in since a specific date.
   *
   * <p>Combines active status filtering with recent login filtering.
   *
   * @param since the cutoff date for last login
   * @return List of active users who logged in after the specified date
   */
  List<User> findByIsActiveTrueAndLastLoginAfter(LocalDateTime since);

  /**
   * Counts the total number of active users.
   *
   * <p>Provides an efficient count without loading user entities into memory.
   *
   * @return the number of active users
   */
  long countByIsActiveTrue();

  /**
   * Counts active users for a specific OAuth provider.
   *
   * @param oauthProvider the OAuth provider (e.g., "facebook", "google")
   * @return the number of active users from the specified provider
   */
  long countByOauthProviderAndIsActiveTrue(String oauthProvider);

  /**
   * Checks if a user exists with the given OAuth provider and ID combination.
   *
   * <p>This is more efficient than {@link #findByOauthProviderAndOauthId(String, String)} when you
   * only need to check existence without loading the entity.
   *
   * @param oauthProvider the OAuth provider (e.g., "facebook", "google")
   * @param oauthId the unique ID from the OAuth provider
   * @return true if a user exists with these credentials, false otherwise
   */
  boolean existsByOauthProviderAndOauthId(String oauthProvider, String oauthId);

  /**
   * Bulk update last login timestamp for multiple users.
   *
   * <p>This custom query allows efficient bulk updates without loading entities. Useful for batch
   * processing login events.
   *
   * @param userIds the UUIDs of users to update
   * @param loginTime the timestamp to set as last login
   * @return the number of users updated
   */
  @Modifying
  @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.id IN :userIds")
  int updateLastLoginForUsers(
      @Param("userIds") List<UUID> userIds, @Param("loginTime") LocalDateTime loginTime);

  /**
   * Bulk deactivate users by their IDs.
   *
   * <p>Sets isActive=false for all users in the provided list. This is more efficient than loading
   * and updating entities individually for bulk operations.
   *
   * @param userIds the UUIDs of users to deactivate
   * @return the number of users deactivated
   */
  @Modifying
  @Query("UPDATE User u SET u.isActive = false WHERE u.id IN :userIds")
  int deactivateUsers(@Param("userIds") List<UUID> userIds);

  /**
   * Bulk activate users by their IDs.
   *
   * <p>Sets isActive=true for all users in the provided list.
   *
   * @param userIds the UUIDs of users to activate
   * @return the number of users activated
   */
  @Modifying
  @Query("UPDATE User u SET u.isActive = true WHERE u.id IN :userIds")
  int activateUsers(@Param("userIds") List<UUID> userIds);
}
