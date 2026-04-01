package demo.bacnaoday.security;

import demo.bacnaoday.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;

    public AppUserDetails(Long userId, String username, String passwordHash) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public static AppUserDetails from(User user) {
        return new AppUserDetails(user.getId(), user.getUsername(), user.getPasswordHash());
    }

    /** Principal rebuilt from a verified JWT (no password). */
    public static AppUserDetails fromJwt(Long userId, String username) {
        return new AppUserDetails(userId, username, "");
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
