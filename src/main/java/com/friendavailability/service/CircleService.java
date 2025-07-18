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
public class CircleService {
    private final CircleRepository circleRepository;
    private final CircleMemberRepository circleMemberRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    public CircleService(CircleMemberRepository circleMemberRepository, CircleRepository circleRepository,
            UserRepository userRepository, FriendRepository friendRepository) {
        this.circleMemberRepository = circleMemberRepository;
        this.circleRepository = circleRepository;
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
    }

    public Circle createCircle(Long creatorId, String name, String description, Integer maxMembers) {
        validateUserExists(creatorId);
        validateCircleName(name);

        if (description != null && description.length() >= 500) {
            throw new RuntimeException("Description can not exceed 500 characters");
        }

        if (circleRepository.existsActiveCircleWithNameForUser(creatorId, name)) {
            throw new RuntimeException("You already have a circle with this name");
        }

        try {
            Circle circle = Circle.builder()
                    .name(name.trim())
                    .description(description != null ? description.trim() : null)
                    .createdBy(creatorId)
                    .maxMembers(maxMembers)
                    .build();

            Circle savedCircle = circleRepository.save(circle);

            addMemberToCircleInternal(savedCircle.getId(), creatorId, CircleRole.OWNER, creatorId);

            return savedCircle;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create circle " + e.getMessage());
        }
    }

    public Circle updateCircle(Long circleId, String newName, String description, Long requestingUserId) {
        validateUserExists(requestingUserId);

        Circle circle = getCircleById(circleId);

        if (!circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId)) {
            throw new RuntimeException("Only circle owners and admins can update circle details");
        }

        if (newName != null) {
            validateCircleName(newName);

            Optional<Circle> existingCircle = circleRepository.findByNameAndCreatedByAndIsActiveTrue(newName.trim(),
                    circle.getCreatedBy());
            if (existingCircle.isPresent() && !existingCircle.get().getId().equals(circleId)) {
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

    public boolean deleteCircle(Long circleId, Long requestingUserId) {
        validateUserExists(requestingUserId);

        Circle circle = getCircleById(circleId);

        if (!circleMemberRepository.isUserOwnerOfCircle(requestingUserId, circleId)) {
            throw new RuntimeException("Only the circle owner can delete the circle");
        }

        try {
            circleMemberRepository.deactivateAllMembershipsForCircle(circleId);
            circleRepository.softDeleteCircle(circleId);

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete circle " + e.getMessage());
        }
    }

    public CircleMember addMemberToCircle(Long circleId, Long userId, Long requestingUserId) {
        validateUserExists(requestingUserId);
        validateUserExists(userId);

        Circle circle = getCircleById(circleId);

        if (circleMemberRepository.isUserActiveMemberOfCircle(userId, circleId)) {
            throw new RuntimeException("User is already a member of the circle");
        }

        if (!circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId)) {
            throw new RuntimeException("Only the owner and admins can add users");
        }

        if (!userId.equals(requestingUserId)) {
            if (!friendRepository.existsFriendshipBetweenUsers(userId, requestingUserId)) {
                throw new RuntimeException("Users must be friends first");
            }
        }

        if (circle.hasMaxMembers()) {
            long currentMemberCount = circleMemberRepository.countActiveMembersInCircle(circleId);
            if (currentMemberCount >= circle.getMaxMembers()) {
                throw new RuntimeException("Circle has reached its maximum member limit");
            }
        }

        return addMemberToCircleInternal(circleId, userId, CircleRole.MEMBER, requestingUserId);
    }

    public boolean removeMemberFromCircle(Long circleId, Long userId, Long requestingUserId) {
        validateUserExists(userId);
        validateUserExists(requestingUserId);

        getCircleById(circleId);

        if (!circleMemberRepository.isUserActiveMemberOfCircle(userId, circleId)) {
            throw new RuntimeException("User is not a member of the circle");
        }

        Optional<CircleMember> memberOpt = circleMemberRepository.findActiveMembershipRecord(userId, circleId);

        if (memberOpt.isEmpty()) {
            throw new RuntimeException("membership not found");
        }

        CircleMember member = memberOpt.get();

        boolean isSelfRemoval = userId.equals(requestingUserId);
        boolean isAdminOrOwner = circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId);

        if (!isSelfRemoval && !isAdminOrOwner) {
            throw new RuntimeException("Only admins, owner and yourself can remove");
        }

        if (member.isOwner()) {
            long ownerCount = circleMemberRepository.countMembersByRole(circleId, CircleRole.OWNER);
            if (ownerCount <= 1) {
                throw new RuntimeException("Either transfer the ownership or delete the circle");
            }
        }

        try {
            circleMemberRepository.deactivateMembership(userId, circleId);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove member from circle: " + e.getMessage());
        }
    }

    public CircleMember updateMemberRole(Long circleId, Long memberId, CircleRole newRole, Long requestingUserId) {
        validateUserExists(memberId);
        validateUserExists(requestingUserId);

        if (newRole == null) {
            throw new RuntimeException("New role can not be null");
        }

        if (!circleMemberRepository.isUserOwnerOfCircle(requestingUserId, circleId)) {
            throw new RuntimeException("Only the owner can update roles");
        }

        if (newRole == CircleRole.OWNER) {
            throw new RuntimeException("Can not have multiple owners. Transfer ownership is required");
        }

        Optional<CircleMember> memberOpt = circleMemberRepository.findActiveMembershipRecord(memberId, circleId);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("User not found with id " + memberId);
        }
        CircleMember member = memberOpt.get();

        if (member.isOwner()) {
            long ownerCount = circleMemberRepository.countMembersByRole(circleId, CircleRole.OWNER);
            if (ownerCount <= 1) {
                throw new RuntimeException("Cannot demote the last owner. Transfer ownership first.");
            }
        }

        try {
            circleMemberRepository.updateMemberRole(member.getId(), newRole);
            return circleMemberRepository.findById(member.getId())
                    .orElseThrow(() -> new RuntimeException("Failed to update member role"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update member role: " + e.getMessage());
        }
    }

    public boolean transferOwnership(Long circleId, Long newOwnerId, Long currentOwnerId) {
        validateUserExists(currentOwnerId);
        validateUserExists(newOwnerId);

        if (!circleMemberRepository.isUserOwnerOfCircle(currentOwnerId, circleId)) {
            throw new RuntimeException("Only the owner can transfer the ownership");
        }

        Optional<CircleMember> memberOpt = circleMemberRepository.findActiveMembershipRecord(newOwnerId, circleId);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("The new owner must be a member of the circle first");
        }

        Optional<CircleMember> ownerOpt = circleMemberRepository.findActiveMembershipRecord(currentOwnerId, circleId);
        if (ownerOpt.isEmpty()) {
            throw new RuntimeException("current owner membership not found");
        }

        try {
            CircleMember newOwner = memberOpt.get();
            CircleMember currentOwner = ownerOpt.get();

            circleMemberRepository.updateMemberRole(newOwner.getId(), CircleRole.OWNER);
            circleMemberRepository.updateMemberRole(currentOwner.getId(), CircleRole.ADMIN);

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to transfer ownership: " + e.getMessage());
        }
    }

    public List<Circle> getCirclesForUser(Long userId) {
        validateUserExists(userId);
        return circleRepository.findCirclesForUser(userId);
    }

    /**
     * Get circles created by a user
     */
    public List<Circle> getCirclesCreatedByUser(Long userId) {
        validateUserExists(userId);
        return circleRepository.findCirclesCreatedByUser(userId);
    }

    /**
     * Get circle members (if user has permission to view)
     */
    public List<CircleMember> getCircleMembers(Long circleId, Long requestingUserId) {
        validateUserExists(requestingUserId);
        getCircleById(circleId); // Validate circle exists

        // Check if user is a member (members can see other members)
        if (!circleMemberRepository.isUserActiveMemberOfCircle(requestingUserId, circleId)) {
            throw new RuntimeException("You must be a member of the circle to view its members");
        }

        return circleMemberRepository.findActiveCircleMembers(circleId);
    }

    /**
     * Search circles by name
     */
    public List<Circle> searchCircles(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }
        return circleRepository.findActiveCirclesByNameContaining(searchTerm.trim());
    }

    // ========== UTILITY METHODS ==========

    /**
     * Internal method to add member without all the business rule checks
     * Used during circle creation and when business rules are already validated
     */
    private CircleMember addMemberToCircleInternal(Long circleId, Long userId, CircleRole role, Long invitedBy) {
        // Check if membership already exists (might be inactive)
        Optional<CircleMember> existingMembershipOpt = circleMemberRepository.findMembershipRecord(userId, circleId);

        if (existingMembershipOpt.isPresent()) {
            // Reactivate existing membership
            CircleMember existingMembership = existingMembershipOpt.get();
            existingMembership.reactivate();
            existingMembership.updateRole(role);
            return circleMemberRepository.save(existingMembership);
        } else {
            // Create new membership
            User user = getUserById(userId);
            Circle circle = getCircleById(circleId);

            CircleMember membership = CircleMember.builder()
                    .user(user)
                    .circle(circle)
                    .userId(userId)
                    .circleId(circleId)
                    .role(role)
                    .invitedBy(invitedBy)
                    .build();

            return circleMemberRepository.save(membership);
        }
    }

    private Circle getCircleById(Long circleId) {
        return circleRepository.findById(circleId)
                .filter(Circle::isActiveCircle)
                .orElseThrow(() -> new RuntimeException("Circle not found with ID: " + circleId));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    private void validateUserExists(Long userId) {
        if (userId == null) {
            throw new RuntimeException("User ID cannot be null");
        }
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
    }

    private void validateCircleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Circle name cannot be empty");
        }
        if (name.trim().length() < 2) {
            throw new RuntimeException("Circle name must be at least 2 characters long");
        }
        if (name.trim().length() > 100) {
            throw new RuntimeException("Circle name cannot exceed 100 characters");
        }
    }
}