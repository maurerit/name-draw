package com.namedraw.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for User entity.
 *
 * <p>Represents user information in API responses. Contains only public user data suitable for
 * sharing with other users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  private UUID id;
  private String name;
  private String profilePictureUrl;
  private Boolean isActive;
}
