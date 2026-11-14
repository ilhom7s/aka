package com.analytics.service.repository;

import com.analytics.service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUserPhone(String userPhone);

    /** Строка пользователя до конца транзакции. Сериализует правки его корзины, в том числе первый INSERT. */
    @Query(value = "SELECT id FROM users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);
    boolean existsByUserPhone(String userPhone);

    boolean existsByEmail(String email);
}
