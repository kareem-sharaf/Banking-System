package com.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Role Permission Entity (Many-to-Many relationship between Role and Permission)
 */
@Entity
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_id", columnList = "role_id"),
    @Index(name = "idx_permission_id", columnList = "permission_id"),
    @Index(name = "idx_role_permission", columnList = "role_id, permission_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}

