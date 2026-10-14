package com.udea.business.security;

import java.util.List;

public interface IJwtService {
    String extractUserId(String token);
    String extractEmail(String token);
    List<String> extractRoles(String token);
    String extractRolActivo(String token);
    boolean isTokenValid(String token);
}
