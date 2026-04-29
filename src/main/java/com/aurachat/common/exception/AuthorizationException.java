package com.aurachat.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a user attempts to access a resource
 * without the required permissions.
 * Contains information about the resource and the required permission.
 */
@Getter
public class AuthorizationException extends CustomException {
    
    /**
     * The resource that the user attempted to access
     */
    private final String resource;
    
    /**
     * The permission required to access the resource
     */
    private final String requiredPermission;
    
    /**
     * The current user who attempted the access (optional)
     */
    private final String currentUser;
    
    /**
     * Constructs a new AuthorizationException with resource and required permission.
     *
     * @param resource the resource that was attempted to be accessed
     * @param requiredPermission the permission required to access the resource
     */
    public AuthorizationException(String resource, String requiredPermission) {
        super(ErrorCode.ACCESS_DENIED.getCode(), "Access denied to resource: " + resource);
        this.resource = resource;
        this.requiredPermission = requiredPermission;
        this.currentUser = null;
    }
    
    /**
     * Constructs a new AuthorizationException with resource, required permission, and current user.
     *
     * @param resource the resource that was attempted to be accessed
     * @param requiredPermission the permission required to access the resource
     * @param currentUser the user who attempted the access
     */
    public AuthorizationException(String resource, String requiredPermission, String currentUser) {
        super(ErrorCode.ACCESS_DENIED.getCode(), 
              String.format("User '%s' does not have permission '%s' to access resource: %s", 
                           currentUser, requiredPermission, resource));
        this.resource = resource;
        this.requiredPermission = requiredPermission;
        this.currentUser = currentUser;
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, resource=%s, requiredPermission=%s, currentUser=%s, message=%s, context=%s]",
                getClass().getSimpleName(),
                getErrorCode(),
                resource,
                requiredPermission,
                currentUser,
                getMessage(),
                getContext());
    }
}
