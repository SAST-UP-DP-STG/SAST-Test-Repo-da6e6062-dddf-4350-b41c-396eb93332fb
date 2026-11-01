package com.example.userapp.repository;

import com.example.userapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.passwordHash = :hash WHERE u.id = :userId")
    int updatePasswordHash(@Param("userId") Long userId, @Param("hash") String hash);
}
