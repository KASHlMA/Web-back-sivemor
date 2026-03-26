package com.sivemor.platform.security

import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AppUserPrincipal(
    val id: Long,
    private val usernameValue: String,
    val role: Role,
    private val passwordValue: String,
    private val active: Boolean
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String = passwordValue

    override fun getUsername(): String = usernameValue

    override fun isEnabled(): Boolean = active

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    companion object {
        fun from(user: User): AppUserPrincipal = AppUserPrincipal(
            id = user.id ?: 0L,
            usernameValue = user.username,
            role = user.role,
            passwordValue = user.passwordHash,
            active = user.active && !user.archived
        )
    }
}
