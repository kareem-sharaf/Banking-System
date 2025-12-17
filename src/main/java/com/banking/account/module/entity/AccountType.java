package com.banking.account.module.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Account Type Entity
 */
@Entity
@Table(name = "account_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "accountType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();
}
