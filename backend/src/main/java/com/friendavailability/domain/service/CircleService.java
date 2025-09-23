package com.friendavailability.domain.service;

import com.friendavailability.domain.entity.Circle;
import com.friendavailability.domain.entity.CircleMember;
import com.friendavailability.domain.entity.enums.CircleRole;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.*;
import com.friendavailability.domain.repository.CircleRepository;
import com.friendavailability.domain.repository.CircleMemberRepository;
import com.friendavailability.domain.repository.FriendRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class CircleService {

    private final CircleRepository circleRepository;
    private final CircleMemberRepository circleMemberRepository;
    private final FriendRepository friendRepository;
    private final UserService userService;

    public CircleService(CircleMemberRepository circleMemberRepository, CircleRepository circleRepository,
                         FriendRepository friendRepository, UserService userService) {
        this.circleMemberRepository = circleMemberRepository;
        this.circleRepository = circleRepository;
        this.friendRepository = friendRepository;
        this.userService = userService;
        log.info("CircleService created");
    }

    public Circle createCircle(Long creatorId, String name, String description, Integer maxMembers) {
        log.debug("Creating circle '{}' for user {}", name, creatorId);

        User creator = userService.findUserById(creatorId);
        validateCircleName(name);

        if (description != null && description.length() >= 500) {
            log.warn("Circle description too long: {} characters", description.length());
            throw ValidationException.circleDescriptionTooLong(500);
        }

        if (circleRepository.existsActiveCircleWithNameForUser(creatorId, name)) {
            log.warn("Duplicate circle name '{}' for user {}", name, creatorId);
            throw DuplicateResourceException.duplicateCircleName(name);
        }

        Circle circle = Circle.builder()
                .name(name.trim())
                .description(description != null ? description.trim() : null)
                .createdBy(creatorId)
                .maxMembers(maxMembers)
                .build();

        Circle savedCircle = circleRepository.save(circle);
        addMemberToCircleInternal(savedCircle.getId(), creatorId, CircleRole.OWNER, creatorId);

        log.info("Circle '{}' created successfully with ID {}", name, savedCircle.getId());
        return savedCircle;
    }

    public Circle updateCircle(Long circleId, String newName, String description, Long requestingUserId) {
        log.debug("Updating circle {} by user {}", circleId, requestingUserId);

        User requestingUser = userService.findUserById(requestingUserId);
        Circle circle = findCircleById(circleId);

        if (!circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId)) {
            log.warn("User {} attempted to update circle {} without permission", requestingUserId, circleId);
            throw InsufficientPermissionException.onlyAdminCanUpdate();
        }

        if (newName != null) {
            validateCircleName(newName);

            Optional<Circle> existingCircle = circleRepository.findByNameAndCreatedByAndIsActiveTrue(newName.trim(),
                    circle.getCreatedBy());
            if (existingCircle.isPresent() && !existingCircle.get().getId().equals(circleId)) {
                log.warn("Circle name '{}' already exists for user {}", newName, circle.getCreatedBy());
                throw DuplicateResourceException.duplicateCircleName(newName);
            }

            circle.setName(newName.trim());
        }

        if (description != null) {
            if (description.length() > 500) {
                log.warn("Circle description too long: {} characters", description.length());
                throw ValidationException.circleDescriptionTooLong(500);
            }
            circle.setDescription(description.trim());
        }

        Circle updatedCircle = circleRepository.save(circle);
        log.info("Circle {} updated successfully", circleId);
        return updatedCircle;
    }

    public void deleteCircle(Long circleId, Long requestingUserId) {
        log.debug("Deleting circle {} by user {}", circleId, requestingUserId);

        User requestingUser = userService.findUserById(requestingUserId);
        Circle circle = findCircleById(circleId);

        if (!circleMemberRepository.isUserOwnerOfCircle(requestingUserId, circleId)) {
            log.warn("User {} attempted to delete circle {} without permission", requestingUserId, circleId);
            throw InsufficientPermissionException.onlyOwnerCanDelete();
        }

        circleMemberRepository.deactivateAllMembershipsForCircle(circleId);
        circleRepository.softDeleteCircle(circleId);

        log.info("Circle {} deleted successfully", circleId);
    }

    public CircleMember addMemberToCircle(Long circleId, Long userId, Long requestingUserId) {
        log.debug("Adding user {} to circle {} by user {}", userId, circleId, requestingUserId);

        User requestingUser = userService.findUserById(requestingUserId);
        User userToAdd = userService.findUserById(userId);
        Circle circle = findCircleById(circleId);

        if (circleMemberRepository.isUserActiveMemberOfCircle(userId, circleId)) {
            log.warn("User {} is already a member of circle {}", userId, circleId);
            throw InvalidOperationException.userAlreadyMember();
        }

        if (!circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId)) {
            log.warn("User {} attempted to add member to circle {} without permission", requestingUserId, circleId);
            throw InsufficientPermissionException.onlyAdminCanAddMembers();
        }

        if (!userId.equals(requestingUserId)) {
            if (!friendRepository.existsFriendshipBetweenUsers(userId, requestingUserId)) {
                log.warn("Users {} and {} are not friends, cannot add to circle", userId, requestingUserId);
                throw InvalidOperationException.mustBeFriendsFirst();
            }
        }

        if (circle.hasMaxMembers()) {
            long currentMemberCount = circleMemberRepository.countActiveMembersInCircle(circleId);
            if (currentMemberCount >= circle.getMaxMembers()) {
                log.warn("Circle {} has reached maximum capacity: {}", circleId, circle.getMaxMembers());
                throw InvalidOperationException.circleAtMaxCapacity();
            }
        }

        CircleMember newMember = addMemberToCircleInternal(circleId, userId, CircleRole.MEMBER, requestingUserId);
        log.info("User {} added to circle {} successfully", userId, circleId);
        return newMember;
    }

    public void removeMemberFromCircle(Long circleId, Long userId, Long requestingUserId) {
        log.debug("Removing user {} from circle {} by user {}", userId, circleId, requestingUserId);

        User requestingUser = userService.findUserById(requestingUserId);
        User userToRemove = userService.findUserById(userId);
        Circle circle = findCircleById(circleId);

        CircleMember member = findActiveMembershipRecord(userId, circleId);

        boolean isSelfRemoval = userId.equals(requestingUserId);
        boolean isAdminOrOwner = circleMemberRepository.isUserAdminOrOwnerOfCircle(requestingUserId, circleId);

        if (!isSelfRemoval && !isAdminOrOwner) {
            log.warn("User {} attempted to remove user {} from circle {} without permission",
                    requestingUserId, userId, circleId);
            throw InsufficientPermissionException.onlyAdminCanRemoveMembers();
        }

        if (member.isOwner()) {
            long ownerCount = circleMemberRepository.countMembersByRole(circleId, CircleRole.OWNER);
            if (ownerCount <= 1) {
                log.warn("Cannot remove last owner from circle {}", circleId);
                throw InvalidOperationException.cannotRemoveLastOwner();
            }
        }

        circleMemberRepository.deactivateMembership(userId, circleId);
        log.info("User {} removed from circle {} successfully", userId, circleId);
    }

    public CircleMember updateMemberRole(Long circleId, Long memberId, CircleRole newRole, Long requestingUserId) {
        log.debug("Updating role of user {} in circle {} to {} by user {}", memberId, circleId, newRole, requestingUserId);

        User requestingUser = userService.findUserById(requestingUserId);
        User memberUser = userService.findUserById(memberId);

        if (newRole == null) {
            throw ValidationException.invalidFieldValue("role", "New role cannot be null");
        }

        if (!circleMemberRepository.isUserOwnerOfCircle(requestingUserId, circleId)) {
            log.warn("User {} attempted to update role in circle {} without permission", requestingUserId, circleId);
            throw InsufficientPermissionException.onlyOwnerCanUpdateRoles();
        }

        if (newRole == CircleRole.OWNER) {
            log.warn("Cannot directly assign owner role, must transfer ownership");
            throw InvalidOperationException.cannotDirectlyAssignOwner();
        }

        CircleMember member = findActiveMembershipRecord(memberId, circleId);

        if (member.isOwner()) {
            long ownerCount = circleMemberRepository.countMembersByRole(circleId, CircleRole.OWNER);
            if (ownerCount <= 1) {
                log.warn("Cannot demote last owner in circle {}", circleId);
                throw InvalidOperationException.cannotDemoteLastOwner();
            }
        }

        circleMemberRepository.updateMemberRole(member.getId(), newRole);
        CircleMember updatedMember = circleMemberRepository.findById(member.getId())
                .orElseThrow(() -> {
                    log.error("Failed to find updated member {} after role update", member.getId());
                    return ResourceNotFoundException.circleMemberNotFound(memberId, circleId);
                });

        log.info("Role of user {} in circle {} updated to {} successfully", memberId, circleId, newRole);
        return updatedMember;
    }

    public void transferOwnership(Long circleId, Long newOwnerId, Long currentOwnerId) {
        log.debug("Transferring ownership of circle {} from user {} to user {}", circleId, currentOwnerId, newOwnerId);

        User currentOwner = userService.findUserById(currentOwnerId);
        User newOwner = userService.findUserById(newOwnerId);

        if (!circleMemberRepository.isUserOwnerOfCircle(currentOwnerId, circleId)) {
            log.warn("User {} attempted to transfer ownership but is not the owner of circle {}", currentOwnerId, circleId);
            throw InsufficientPermissionException.onlyOwnerCanTransferOwnership();
        }

        CircleMember newOwnerMember = findActiveMembershipRecord(newOwnerId, circleId);
        CircleMember currentOwnerMember = findActiveMembershipRecord(currentOwnerId, circleId);

        circleMemberRepository.updateMemberRole(newOwnerMember.getId(), CircleRole.OWNER);
        circleMemberRepository.updateMemberRole(currentOwnerMember.getId(), CircleRole.ADMIN);

        log.info("Ownership of circle {} transferred from user {} to user {} successfully",
                circleId, currentOwnerId, newOwnerId);
    }

    public List<Circle> getCirclesForUser(Long userId) {
        log.debug("Getting circles for user {}", userId);

        User user = userService.findUserById(userId);
        List<Circle> circles = circleRepository.findCirclesForUser(userId);

        log.debug("Found {} circles for user {}", circles.size(), userId);
        return circles;
    }

    public List<Circle> getCirclesCreatedByUser(Long userId) {
        log.debug("Getting circles created by user {}", userId);

        User user = userService.findUserById(userId);
        List<Circle> circles = circleRepository.findCirclesCreatedByUser(userId);

        log.debug("Found {} circles created by user {}", circles.size(), userId);
        return circles;
    }

    public List<CircleMember> getCircleMembers(Long circleId, Long requestingUserId) {
        log.debug("Getting members of circle {} for user {}", circleId, requestingUserId);

        User requestingUser = userService.findUserById(requestingUserId);
        Circle circle = findCircleById(circleId);

        if (!circleMemberRepository.isUserActiveMemberOfCircle(requestingUserId, circleId)) {
            log.warn("User {} attempted to view members of circle {} without permission", requestingUserId, circleId);
            throw InsufficientPermissionException.mustBeMemberToView();
        }

        List<CircleMember> members = circleMemberRepository.findActiveCircleMembers(circleId);
        log.debug("Found {} members in circle {}", members.size(), circleId);
        return members;
    }

    public List<Circle> searchCircles(String searchTerm) {
        log.debug("Searching circles with term: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        List<Circle> circles = circleRepository.findActiveCirclesByNameContaining(searchTerm.trim());
        log.debug("Found {} circles matching search term '{}'", circles.size(), searchTerm);
        return circles;
    }

    public Circle getCircle(Long circleId) {
        log.debug("Getting circle {}", circleId);
        return findCircleById(circleId);
    }

    public boolean canUserAccessCircle(Long circleId, Long userId) {
        log.debug("Checking if user {} can access circle {}", userId, circleId);

        if (userId == null || circleId == null) {
            return false;
        }

        try {
            userService.findUserById(userId);
            return circleMemberRepository.isUserActiveMemberOfCircle(userId, circleId);
        } catch (ResourceNotFoundException e) {
            log.debug("User {} not found, cannot access circle {}", userId, circleId);
            return false;
        }
    }

    public CircleMember getUserMembershipInCircle(Long circleId, Long userId) {
        log.debug("Getting membership of user {} in circle {}", userId, circleId);

        User user = userService.findUserById(userId);
        return findActiveMembershipRecord(userId, circleId);
    }

    public long getMemberCountForCircle(Long circleId) {
        log.debug("Getting member count for circle {}", circleId);

        long count = circleMemberRepository.countActiveMembersInCircle(circleId);
        log.debug("Circle {} has {} members", circleId, count);
        return count;
    }

    private CircleMember addMemberToCircleInternal(Long circleId, Long userId, CircleRole role, Long invitedBy) {
        Optional<CircleMember> existingMembershipOpt = circleMemberRepository.findMembershipRecord(userId, circleId);

        if (existingMembershipOpt.isPresent()) {
            CircleMember existingMembership = existingMembershipOpt.get();
            existingMembership.reactivate();
            existingMembership.updateRole(role);
            return circleMemberRepository.save(existingMembership);
        } else {
            User user = userService.findUserById(userId);
            Circle circle = findCircleById(circleId);

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

    private Circle findCircleById(Long circleId) {
        return circleRepository.findById(circleId)
                .filter(Circle::isActiveCircle)
                .orElseThrow(() -> {
                    log.warn("Circle not found with ID: {}", circleId);
                    return ResourceNotFoundException.circleNotFound(circleId);
                });
    }

    private CircleMember findActiveMembershipRecord(Long userId, Long circleId) {
        return circleMemberRepository.findActiveMembershipRecord(userId, circleId)
                .orElseThrow(() -> {
                    log.warn("Active membership not found for user {} in circle {}", userId, circleId);
                    return ResourceNotFoundException.circleMemberNotFound(userId, circleId);
                });
    }

    private void validateCircleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw ValidationException.circleNameRequired();
        }
        if (name.trim().length() < 2) {
            throw ValidationException.circleNameTooShort(2);
        }
        if (name.trim().length() > 100) {
            throw ValidationException.circleNameTooLong(100);
        }
    }
}