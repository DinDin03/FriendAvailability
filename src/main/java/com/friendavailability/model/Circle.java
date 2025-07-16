package com.friendavailability.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "circles", indexes = {
    @Index(name = "idx_circle_created_by", columnList = "created_by"),
    @Index(name = "idx_circle_created_at", columnList = "created_at"),
    @Index(name = "")
})
