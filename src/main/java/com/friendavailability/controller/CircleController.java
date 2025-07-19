package com.friendavailability.controller;

import com.friendavailability.dto.circle.*;
import com.friendavailability.model.Circle;
import com.friendavailability.model.CircleMember;
import com.friendavailability.model.CircleRole;
import com.friendavailability.service.CircleService;
import com.friendavailability.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/circles")
@CrossOrigin(origins = "*")
public class CircleController {

    private final CircleService circleService;
    private final UserService userService;

    public CircleController(CircleService circleService, UserService userService) {
        this.circleService = circleService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createCircle(@RequestBody CreateCircleRequest request, @RequestParam Long userId) {
        try {
            Circle circle = circleService.createCircle(userId, request.getName(), request.getDescription(),
                    request.getMaxMembers());

            return ResponseEntity.ok(Map.of(
                    "message", "Circle created successfully",
                    "circle", Map.of(
                            "id", circle.getId(),
                            "name", circle.getName(),
                            "description", circle.getDescription(),
                            "createdBy", circle.getCreatedBy(),
                            "maxMembers", circle.getMaxMembers(),
                            "circleColor", circle.getCircleColor(),
                            "createdAt", circle.getCreatedAt(),
                            "memberCount", 1)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "CREATE_CIRCLE_FAILED"));
        }
    }

    @PutMapping("/{circleId}")
    public ResponseEntity<?> updateCircle(@PathVariable Long circleId, @RequestBody UpdateCircleRequest request, @RequestParam Long userId){
        try{
            Circle circle = circleService.updateCircle(circleId, request.getName(), request.getDescription(), userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Circle updated successfully",
                    "circle", Map.of(
                            "id", circle.getId(),
                            "name", circle.getName(),
                            "description", circle.getDescription(),
                            "updatedAt", circle.getUpdatedAt()
                    )));
        }catch(Exception error) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", error.getMessage(),
                    "errorCode", "UPDATE_CIRCLE_FAILED"));
        }
    }

    @DeleteMapping("/{circleId}")



}
