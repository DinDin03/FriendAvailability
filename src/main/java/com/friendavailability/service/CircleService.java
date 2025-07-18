package com.friendavailability.service;

import com.friendavailability.model.Circle;
import com.friendavailability.model.CircleMember;
import com.friendavailability.model.CircleRole;
import com.friendavailability.model.Friend;
import com.friendavailability.model.User;
import com.friendavailability.repository.CircleRepository;
import com.friendavailability.repository.CircleMemberRepository;
import com.friendavailability.repository.UserRepository;
import com.friendavailability.repository.FriendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CircleService{
    private final CircleRepository circleRepository;
    private final CircleMemberRepository circleMemberRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    public CircleService(CircleMemberRepository circleMemberRepository, CircleRepository circleRepository, UserRepository userRepository, FriendRepository friendRepository){
        this.circleMemberRepository = circleMemberRepository;
        this.circleRepository = circleRepository;
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
    }

    public Circle createCircle(Long creatorId, String name, String description,  Integer maxMembers){
        validateUserExists(userId);
        validateCircleName(name);

        if(description != null && description.length() >= 500){
            throw new RuntimeException("Description can not exceed 500 characters");
        }

        if(circleRepository.existsActiveCircleWithNameForUser(creatorId, name)){
            throw new RuntimeException("You already have a circle with this name");
        }

        try{
            Circle circle = Circle.builder()
                    .name(name.trim())
                    .description(description != null ? description.trim() : null)
                    .createdBy(creatorId)
                    .maxMembers(maxMembers)
                    .build();
            
            Circle savedCircle = circleRepository.save(circle);

            addMemberToCircleInternal(savedCircle.getId(), creatorId, CircleRole.OWNER, creatorId);

            return savedCircle;
        }catch(Exception e){
            throw new RuntimeException("Failed to create circle " + e.getMessage());
        }
    }

    public Circle updateCircle(Long circleId, String newName, String description, Long requestingUserId){
        validateUserExists(requestingUserId);
        
        Circle circle = getCircleById(circleId);

        if(!circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId)){
            throw new RuntimeException("Only circle owners and admins can update circle details");
        }
        
        if(newName != null){
            validateCircleName(newName);

            Optional<Circle> existingCircle = circleRepository.findActiveCirclesByNameContaining(newName.trim(), circle.getCreatedBy());
            if(existingCircle.isPresent() && !existingCircle.get().getId().equals(circleId)){
                throw new RuntimeException("you already have a circlr with this name");
            }

            circle.setName(newName.trim());
        }
        if (description != null) {
            if (description.length() > 500) {
                throw new RuntimeException("Circle description can not exceed 500 characters");
            }
            circle.setDescription(description.trim());
        }

        return circleRepository.save(circle);
    }

    public boolean deleteCircle(Long circleId, Long requestingUserId){
        validateUserExists(requestingUserId);

        Circle circle = getCircleById(circleId);

        if(!circleMemberRepository.isUserOwnerOfCircle(requestingUserId, circleId)){
            throw new RuntimeException("Only the circle owner can delete the circle");
        }

        try{
            circleMemberRepository.deactivateAllMembershipsForCircle(circleId);
            circleRepository.softDeleteCircle(circleId);

            return true;
        }catch(Exception e){
            throw new RuntimeException("Failed to delete circle " + e.getMessage());
        }
    }

    public CircleMember addMemberToCircle(Long circleId, Long userId, Long requestingUserId){
        validateUserExists(requestingUserId);
        validateUserExists(userId);

        Circle circle = getCircleById(circleId);

        
    }
}