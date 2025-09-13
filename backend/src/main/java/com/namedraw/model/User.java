package com.namedraw.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Represents authenticated individuals via OAuth social login.
 *
 * <p>This entity stores user information obtained from OAuth providers (Facebook, Google) and
 * tracks user authentication history and status.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"oauth_provider", "oauth_id"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @NotBlank
  @Column(name = "oauth_provider", nullable = false)
  private String oauthProvider;

  @NotNull
  @NotBlank
  @Column(name = "oauth_id", nullable = false)
  private String oauthId;

  @NotNull
  @NotBlank
  @Column(name = "name", nullable = false)
  private String name;

  @Email
  @Column(name = "email")
  private String email;

  @Column(name = "profile_picture_url")
  private String profilePictureUrl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  @NotNull
  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  /** Updates the last login timestamp to the current time. */
  public void updateLastLogin() {
    this.lastLogin = LocalDateTime.now();
  }

  /** Deactivates the user account. */
  public void deactivate() {
    this.isActive = false;
  }

  /** Activates the user account. */
  public void activate() {
    this.isActive = true;
  }
}
