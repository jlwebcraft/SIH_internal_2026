package com.sih.supplychain.repository;

import com.sih.supplychain.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRoleId(Long roleId);

    List<User> findByActive(boolean active);
}
