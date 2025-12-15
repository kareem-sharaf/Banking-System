package com.banking.repository;

import com.banking.entity.LoginHistory;
import com.banking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserOrderByLoginTimeDesc(User user);

    List<LoginHistory> findByUserAndSuccessOrderByLoginTimeDesc(User user, boolean success);

    List<LoginHistory> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByUserAndSuccess(User user, boolean success);
}
