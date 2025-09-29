package com.linkups.domain.repository;

import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.entity.enums.UserStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    Optional<UserStatus> findByUserId(Long userId);

}
