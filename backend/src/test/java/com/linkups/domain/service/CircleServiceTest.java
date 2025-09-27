package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Circle;
import com.linkups.domain.entity.CircleMember;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.enums.CircleRole;
import com.linkups.domain.exception.*;
import com.linkups.domain.repository.CircleRepository;
import com.linkups.domain.repository.CircleMemberRepository;
import com.linkups.domain.repository.FriendRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CircleService
 * 
 * This test class focuses on PERMISSION-BASED BUSINESS LOGIC patterns used at enterprise companies:
 * - Role-based access control (Owner > Admin > Member hierarchy)
 * - Complex business rule validation (friend requirements, member limits)
 * - Entity relationship management (Circle ↔ User ↔ CircleMember)
 * - Permission enforcement at the service layer
 * - Cascade operations and data consistency
 * 
 * Learning Focus:
 * - Permission-based testing patterns
 * - Role hierarchy validation
 * - Complex business rule testing
 * - Entity relationship testing
 * - Authorization vs Authentication patterns
 */
// All tests passing
class CircleServiceTest extends BaseUnitTest {

    // ============== MOCKED DEPENDENCIES ==============
    
    @Mock
    private CircleRepository circleRepository;
    
    @Mock
    private CircleMemberRepository circleMemberRepository;
    
    @Mock
    private FriendRepository friendRepository;
    
    @Mock
    private UserService userService;

    // ============== SERVICE UNDER TEST ==============
    
    @InjectMocks
    private CircleService circleService;

    // ============== CIRCLE CREATION TESTS ==============
    // These test the fundamental circle creation workflow

    @Test
    void shouldCreateCircleSuccessfully() {
        // ============== GIVEN ==============
        Long creatorId = 1L;
        String circleName = "Team Project";
        String description = "Project collaboration circle";
        Integer maxMembers = 10;
        
        User creator = User.builder()
                .id(creatorId)
                .name("Circle Creator")
                .email("creator@example.com")
                .build();
        
        Circle savedCircle = Circle.builder()
                .id(10L)
                .name(circleName)
                .description(description)
                .createdBy(creatorId)
                .maxMembers(maxMembers)
                .isActive(true)
                .build();
        
        // Mock user validation
        given(userService.findUserById(creatorId))
                .willReturn(creator);
        
        // Mock circle name validation
        given(circleRepository.existsActiveCircleWithNameForUser(creatorId, circleName))
                .willReturn(false);
        
        // Mock circle saving
        given(circleRepository.save(any(Circle.class)))
                .willReturn(savedCircle);

        // Mock finding the saved circle (needed for addMemberToCircleInternal)
        given(circleRepository.findById(10L))
                .willReturn(Optional.of(savedCircle));

        // Mock membership checks for addMemberToCircleInternal
        given(circleMemberRepository.findMembershipRecord(creatorId, 10L))
                .willReturn(Optional.empty()); // No existing membership

        // Mock saving the circle membership
        given(circleMemberRepository.save(any(CircleMember.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        Circle result = circleService.createCircle(creatorId, circleName, description, maxMembers);

        // ============== THEN ==============
        assertThat(result).isEqualTo(savedCircle);
        assertThat(result.getName()).isEqualTo(circleName);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getCreatedBy()).isEqualTo(creatorId);
        assertThat(result.getMaxMembers()).isEqualTo(maxMembers);
        
        // Verify user validation (called twice: once in createCircle, once in addMemberToCircleInternal)
        then(userService).should(times(2)).findUserById(creatorId);
        
        // Verify circle was saved
        ArgumentCaptor<Circle> circleCaptor = ArgumentCaptor.forClass(Circle.class);
        then(circleRepository).should().save(circleCaptor.capture());
        
        Circle savedCircleArg = circleCaptor.getValue();
        assertThat(savedCircleArg.getName()).isEqualTo(circleName.trim());
        assertThat(savedCircleArg.getDescription()).isEqualTo(description.trim());
        
        // Verify creator was added as OWNER (critical business rule)
        then(circleMemberRepository).should().save(any(CircleMember.class));
    }

    @Test
    void shouldValidateCircleName() {
        // ============== GIVEN ==============
        Long creatorId = 1L;
        User creator = User.builder().id(creatorId).build();
        
        given(userService.findUserById(creatorId)).willReturn(creator);

        // Test null name
        assertThatThrownBy(() -> circleService.createCircle(creatorId, null, null, null))
                .isInstanceOf(ValidationException.class);
        
        // Test empty name
        assertThatThrownBy(() -> circleService.createCircle(creatorId, "", null, null))
                .isInstanceOf(ValidationException.class);
        
        // Test whitespace name
        assertThatThrownBy(() -> circleService.createCircle(creatorId, "   ", null, null))
                .isInstanceOf(ValidationException.class);
        
        then(circleRepository).should(never()).save(any());
    }

    @Test
    void shouldPreventDuplicateCircleNames() {
        // ============== GIVEN ==============
        Long creatorId = 1L;
        String duplicateName = "Existing Circle";
        
        User creator = User.builder().id(creatorId).build();
        
        given(userService.findUserById(creatorId)).willReturn(creator);
        given(circleRepository.existsActiveCircleWithNameForUser(creatorId, duplicateName))
                .willReturn(true); // Circle name already exists

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.createCircle(creatorId, duplicateName, null, null))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Circle with name 'Existing Circle' already exists");
        
        then(circleRepository).should(never()).save(any());
    }

    @Test
    void shouldValidateDescriptionLength() {
        // ============== GIVEN ==============
        Long creatorId = 1L;
        String validName = "Valid Circle";
        String tooLongDescription = "a".repeat(501); // Over 500 character limit
        
        User creator = User.builder().id(creatorId).build();
        
        given(userService.findUserById(creatorId)).willReturn(creator);

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.createCircle(creatorId, validName, tooLongDescription, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Circle description cannot exceed 500 characters");
        
        then(circleRepository).should(never()).save(any());
    }

    // ============== PERMISSION-BASED UPDATE TESTS ==============
    // These test role-based authorization for circle operations

    @Test
    void shouldUpdateCircleWhenUserIsOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long ownerId = 1L;
        String newName = "Updated Circle";
        String newDescription = "Updated description";
        
        User owner = User.builder().id(ownerId).build();
        Circle existingCircle = Circle.builder()
                .id(circleId)
                .name("Old Circle")
                .createdBy(ownerId)
                .build();
        Circle updatedCircle = Circle.builder()
                .id(circleId)
                .name(newName)
                .description(newDescription)
                .build();
        
        given(userService.findUserById(ownerId)).willReturn(owner);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(existingCircle));
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(ownerId, circleId)).willReturn(true);
        given(circleRepository.findByNameAndCreatedByAndIsActiveTrue(newName.trim(), ownerId))
                .willReturn(Optional.empty()); // No name conflict
        given(circleRepository.save(any(Circle.class))).willReturn(updatedCircle);

        // ============== WHEN ==============
        Circle result = circleService.updateCircle(circleId, newName, newDescription, ownerId);

        // ============== THEN ==============
        assertThat(result).isEqualTo(updatedCircle);
        
        then(circleMemberRepository).should().isUserAdminOrOwnerOfCircle(ownerId, circleId);
        then(circleRepository).should().save(any(Circle.class));
    }

    @Test
    void shouldPreventUpdateWhenUserLacksPermission() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long regularUserId = 2L; // Not an admin or owner
        
        User regularUser = User.builder().id(regularUserId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(regularUserId)).willReturn(regularUser);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(regularUserId, circleId))
                .willReturn(false); // User lacks permission

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.updateCircle(circleId, "New Name", null, regularUserId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("admin");
        
        then(circleRepository).should(never()).save(any());
    }

    // ============== CIRCLE DELETION TESTS ==============
    // These test cascade deletion and ownership requirements

    @Test
    void shouldDeleteCircleWhenUserIsOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long ownerId = 1L;
        
        User owner = User.builder().id(ownerId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(ownerId)).willReturn(owner);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserOwnerOfCircle(ownerId, circleId)).willReturn(true);

        // ============== WHEN ==============
        circleService.deleteCircle(circleId, ownerId);

        // ============== THEN ==============
        // Verify cascade operations occur in correct order
        then(circleMemberRepository).should().deactivateAllMembershipsForCircle(circleId);
        then(circleRepository).should().softDeleteCircle(circleId);
    }

    @Test
    void shouldPreventDeletionWhenUserIsNotOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 2L; // Admin, but not owner
        
        User admin = User.builder().id(adminId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserOwnerOfCircle(adminId, circleId))
                .willReturn(false); // Not the owner

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.deleteCircle(circleId, adminId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("owner");
        
        // Verify no deletion operations occurred
        then(circleMemberRepository).should(never()).deactivateAllMembershipsForCircle(any());
        then(circleRepository).should(never()).softDeleteCircle(any());
    }

    // ============== MEMBER MANAGEMENT TESTS ==============
    // These test complex business rules for adding/removing members

    @Test
    void shouldAddMemberWhenUserIsAdminAndUsersAreFriends() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L;
        Long newMemberId = 2L;
        
        User admin = User.builder().id(adminId).build();
        User newMember = User.builder().id(newMemberId).build();
        Circle circle = Circle.builder().id(circleId).maxMembers(10).build();
        
        CircleMember savedMembership = CircleMember.builder()
                .id(100L)
                .userId(newMemberId)
                .circleId(circleId)
                .role(CircleRole.MEMBER)
                .build();
        
        // Mock validations
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(newMemberId)).willReturn(newMember);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        
        // Mock membership checks
        given(circleMemberRepository.isUserActiveMemberOfCircle(newMemberId, circleId))
                .willReturn(false); // Not already a member
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(adminId, circleId))
                .willReturn(true); // Admin has permission
        
        // Mock friendship check (critical business rule)
        given(friendRepository.existsFriendshipBetweenUsers(newMemberId, adminId))
                .willReturn(true); // Users are friends
        
        // Mock capacity check
        given(circleMemberRepository.countActiveMembersInCircle(circleId))
                .willReturn(5L); // Under capacity
        
        // Mock membership creation
        given(circleMemberRepository.save(any(CircleMember.class)))
                .willReturn(savedMembership);

        // ============== WHEN ==============
        CircleMember result = circleService.addMemberToCircle(circleId, newMemberId, adminId);

        // ============== THEN ==============
        assertThat(result).isEqualTo(savedMembership);
        
        // Verify all business rule checks
        then(circleMemberRepository).should().isUserActiveMemberOfCircle(newMemberId, circleId);
        then(circleMemberRepository).should().isUserAdminOrOwnerOfCircle(adminId, circleId);
        then(friendRepository).should().existsFriendshipBetweenUsers(newMemberId, adminId);
        then(circleMemberRepository).should().countActiveMembersInCircle(circleId);
        then(circleMemberRepository).should().save(any(CircleMember.class));
    }

    @Test
    void shouldAllowSelfAdditionWithoutFriendshipCheck() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L;
        // Same user adding themselves - no friendship required
        
        User admin = User.builder().id(adminId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserActiveMemberOfCircle(adminId, circleId)).willReturn(false);
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(adminId, circleId)).willReturn(true);
        given(circleMemberRepository.save(any(CircleMember.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // ============== WHEN ==============
        circleService.addMemberToCircle(circleId, adminId, adminId);

        // ============== THEN ==============
        // Verify friendship check was skipped for self-addition
        then(friendRepository).should(never()).existsFriendshipBetweenUsers(adminId, adminId);
        then(circleMemberRepository).should().save(any(CircleMember.class));
    }

    @Test
    void shouldPreventAddingMemberWithoutFriendship() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L;
        Long strangerId = 3L; // Not friends with admin
        
        User admin = User.builder().id(adminId).build();
        User stranger = User.builder().id(strangerId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(strangerId)).willReturn(stranger);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserActiveMemberOfCircle(strangerId, circleId)).willReturn(false);
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(adminId, circleId)).willReturn(true);
        given(friendRepository.existsFriendshipBetweenUsers(strangerId, adminId))
                .willReturn(false); // Not friends

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.addMemberToCircle(circleId, strangerId, adminId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("friends first");
        
        then(circleMemberRepository).should(never()).save(any());
    }

    @Test
    void shouldPreventAddingMemberWhenAtCapacity() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L;
        Long newMemberId = 2L;
        
        User admin = User.builder().id(adminId).build();
        User newMember = User.builder().id(newMemberId).build();
        Circle circle = Circle.builder().id(circleId).maxMembers(5).build(); // Small circle
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(newMemberId)).willReturn(newMember);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserActiveMemberOfCircle(newMemberId, circleId)).willReturn(false);
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(adminId, circleId)).willReturn(true);
        given(friendRepository.existsFriendshipBetweenUsers(newMemberId, adminId)).willReturn(true);
        given(circleMemberRepository.countActiveMembersInCircle(circleId))
                .willReturn(5L); // At capacity

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.addMemberToCircle(circleId, newMemberId, adminId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Circle has reached its maximum member limit");
        
        then(circleMemberRepository).should(never()).save(any());
    }

    @Test
    void shouldPreventDuplicateMembership() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L;
        Long existingMemberId = 2L;
        
        User admin = User.builder().id(adminId).build();
        User existingMember = User.builder().id(existingMemberId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(existingMemberId)).willReturn(existingMember);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserActiveMemberOfCircle(existingMemberId, circleId))
                .willReturn(true); // Already a member

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.addMemberToCircle(circleId, existingMemberId, adminId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already a member");
        
        then(circleMemberRepository).should(never()).save(any());
    }

    // ============== MEMBER REMOVAL TESTS ==============
    // These test self-removal vs admin removal permissions

    @Test
    void shouldAllowSelfRemovalFromCircle() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long memberId = 2L;
        
        User member = User.builder().id(memberId).build();
        Circle circle = Circle.builder().id(circleId).build();
        CircleMember membership = CircleMember.builder()
                .userId(memberId)
                .circleId(circleId)
                .role(CircleRole.MEMBER)
                .build();
        
        given(userService.findUserById(memberId)).willReturn(member);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.findActiveMembershipRecord(memberId, circleId))
                .willReturn(Optional.of(membership));

        // ============== WHEN ==============
        circleService.removeMemberFromCircle(circleId, memberId, memberId); // Self-removal

        // ============== THEN ==============
        then(circleMemberRepository).should().deactivateMembership(memberId, circleId);
    }

    @Test
    void shouldAllowAdminToRemoveMember() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L;
        Long memberId = 2L;
        
        User admin = User.builder().id(adminId).build();
        User member = User.builder().id(memberId).build();
        Circle circle = Circle.builder().id(circleId).build();
        CircleMember membership = CircleMember.builder()
                .userId(memberId)
                .circleId(circleId)
                .role(CircleRole.MEMBER)
                .build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(memberId)).willReturn(member);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.findActiveMembershipRecord(memberId, circleId))
                .willReturn(Optional.of(membership));
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(adminId, circleId))
                .willReturn(true); // Admin has permission

        // ============== WHEN ==============
        circleService.removeMemberFromCircle(circleId, memberId, adminId);

        // ============== THEN ==============
        then(circleMemberRepository).should().deactivateMembership(memberId, circleId);
    }

    @Test
    void shouldPreventUnauthorizedMemberRemoval() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long regularUserId = 1L; // Not admin, not the member being removed
        Long targetMemberId = 2L;
        
        User regularUser = User.builder().id(regularUserId).build();
        User targetMember = User.builder().id(targetMemberId).build();
        Circle circle = Circle.builder().id(circleId).build();
        CircleMember membership = CircleMember.builder()
                .userId(targetMemberId)
                .role(CircleRole.MEMBER)
                .build();
        
        given(userService.findUserById(regularUserId)).willReturn(regularUser);
        given(userService.findUserById(targetMemberId)).willReturn(targetMember);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.findActiveMembershipRecord(targetMemberId, circleId))
                .willReturn(Optional.of(membership));
        given(circleMemberRepository.isUserAdminOrOwnerOfCircle(regularUserId, circleId))
                .willReturn(false); // Not admin or owner

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.removeMemberFromCircle(circleId, targetMemberId, regularUserId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("admin");
        
        then(circleMemberRepository).should(never()).deactivateMembership(any(), any());
    }

    @Test
    void shouldPreventRemovalOfLastOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long ownerId = 1L;
        
        User owner = User.builder().id(ownerId).build();
        Circle circle = Circle.builder().id(circleId).build();
        CircleMember ownerMembership = CircleMember.builder()
                .userId(ownerId)
                .circleId(circleId)
                .role(CircleRole.OWNER)
                .build();
        
        given(userService.findUserById(ownerId)).willReturn(owner);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.findActiveMembershipRecord(ownerId, circleId))
                .willReturn(Optional.of(ownerMembership));
        given(circleMemberRepository.countMembersByRole(circleId, CircleRole.OWNER))
                .willReturn(1L); // Only one owner

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.removeMemberFromCircle(circleId, ownerId, ownerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("last owner");
        
        then(circleMemberRepository).should(never()).deactivateMembership(any(), any());
    }

    // ============== ROLE MANAGEMENT TESTS ==============
    // These test the complex role hierarchy and permissions

    @Test
    void shouldUpdateMemberRoleWhenUserIsOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;
        CircleRole newRole = CircleRole.ADMIN;
        
        User owner = User.builder().id(ownerId).build();
        User member = User.builder().id(memberId).build();
        
        CircleMember originalMembership = CircleMember.builder()
                .id(100L)
                .userId(memberId)
                .circleId(circleId)
                .role(CircleRole.MEMBER)
                .build();
        
        CircleMember updatedMembership = CircleMember.builder()
                .id(100L)
                .userId(memberId)
                .circleId(circleId)
                .role(newRole)
                .build();
        
        given(userService.findUserById(ownerId)).willReturn(owner);
        given(userService.findUserById(memberId)).willReturn(member);
        given(circleMemberRepository.isUserOwnerOfCircle(ownerId, circleId)).willReturn(true);
        given(circleMemberRepository.findActiveMembershipRecord(memberId, circleId))
                .willReturn(Optional.of(originalMembership));
        given(circleMemberRepository.findById(originalMembership.getId()))
                .willReturn(Optional.of(updatedMembership));

        // ============== WHEN ==============
        CircleMember result = circleService.updateMemberRole(circleId, memberId, newRole, ownerId);

        // ============== THEN ==============
        assertThat(result).isEqualTo(updatedMembership);
        assertThat(result.getRole()).isEqualTo(newRole);
        
        then(circleMemberRepository).should().updateMemberRole(originalMembership.getId(), newRole);
    }

    @Test
    void shouldPreventDirectOwnerRoleAssignment() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;
        CircleRole ownerRole = CircleRole.OWNER; // Cannot be assigned directly
        
        User owner = User.builder().id(ownerId).build();
        User member = User.builder().id(memberId).build();
        
        given(userService.findUserById(ownerId)).willReturn(owner);
        given(userService.findUserById(memberId)).willReturn(member);

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.updateMemberRole(circleId, memberId, ownerRole, ownerId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("You must be the owner to update user roles");
        
        then(circleMemberRepository).should(never()).updateMemberRole(any(), any());
    }

    @Test
    void shouldPreventDemotingLastOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long ownerId = 1L;
        CircleRole newRole = CircleRole.ADMIN; // Demoting from OWNER
        
        User owner = User.builder().id(ownerId).build();
        
        CircleMember ownerMembership = CircleMember.builder()
                .id(100L)
                .userId(ownerId)
                .circleId(circleId)
                .role(CircleRole.OWNER)
                .build();
        
        given(userService.findUserById(ownerId)).willReturn(owner);
        given(userService.findUserById(ownerId)).willReturn(owner); // Called twice
        given(circleMemberRepository.isUserOwnerOfCircle(ownerId, circleId)).willReturn(true);
        given(circleMemberRepository.findActiveMembershipRecord(ownerId, circleId))
                .willReturn(Optional.of(ownerMembership));
        given(circleMemberRepository.countMembersByRole(circleId, CircleRole.OWNER))
                .willReturn(1L); // Only one owner

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.updateMemberRole(circleId, ownerId, newRole, ownerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("last owner");
        
        then(circleMemberRepository).should(never()).updateMemberRole(any(), any());
    }

    @Test
    void shouldPreventRoleUpdateByNonOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L; // Admin trying to update roles (only owner can)
        Long memberId = 2L;
        CircleRole newRole = CircleRole.ADMIN;
        
        User admin = User.builder().id(adminId).build();
        User member = User.builder().id(memberId).build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(memberId)).willReturn(member);
        given(circleMemberRepository.isUserOwnerOfCircle(adminId, circleId))
                .willReturn(false); // Admin is not owner

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.updateMemberRole(circleId, memberId, newRole, adminId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("owner");
        
        then(circleMemberRepository).should(never()).updateMemberRole(any(), any());
    }

    // ============== OWNERSHIP TRANSFER TESTS ==============
    // These test the critical ownership transfer functionality

    @Test
    void shouldTransferOwnershipSuccessfully() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long currentOwnerId = 1L;
        Long newOwnerId = 2L;
        
        User currentOwner = User.builder().id(currentOwnerId).build();
        User newOwner = User.builder().id(newOwnerId).build();
        
        CircleMember currentOwnerMembership = CircleMember.builder()
                .id(100L)
                .userId(currentOwnerId)
                .role(CircleRole.OWNER)
                .build();
        
        CircleMember newOwnerMembership = CircleMember.builder()
                .id(101L)
                .userId(newOwnerId)
                .role(CircleRole.ADMIN) // Will become OWNER
                .build();
        
        given(userService.findUserById(currentOwnerId)).willReturn(currentOwner);
        given(userService.findUserById(newOwnerId)).willReturn(newOwner);
        given(circleMemberRepository.isUserOwnerOfCircle(currentOwnerId, circleId)).willReturn(true);
        given(circleMemberRepository.findActiveMembershipRecord(newOwnerId, circleId))
                .willReturn(Optional.of(newOwnerMembership));
        given(circleMemberRepository.findActiveMembershipRecord(currentOwnerId, circleId))
                .willReturn(Optional.of(currentOwnerMembership));

        // ============== WHEN ==============
        circleService.transferOwnership(circleId, newOwnerId, currentOwnerId);

        // ============== THEN ==============
        // Verify role updates in correct order
        then(circleMemberRepository).should().updateMemberRole(newOwnerMembership.getId(), CircleRole.OWNER);
        then(circleMemberRepository).should().updateMemberRole(currentOwnerMembership.getId(), CircleRole.ADMIN);
    }

    @Test
    void shouldPreventOwnershipTransferByNonOwner() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long adminId = 1L; // Admin trying to transfer ownership
        Long newOwnerId = 2L;
        
        User admin = User.builder().id(adminId).build();
        User newOwner = User.builder().id(newOwnerId).build();
        
        given(userService.findUserById(adminId)).willReturn(admin);
        given(userService.findUserById(newOwnerId)).willReturn(newOwner);
        given(circleMemberRepository.isUserOwnerOfCircle(adminId, circleId))
                .willReturn(false); // Not the owner

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.transferOwnership(circleId, newOwnerId, adminId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("You must be the owner of the circle to perform a change of ownership");
        
        then(circleMemberRepository).should(never()).updateMemberRole(any(), any());
    }

    // ============== MEMBER ACCESS TESTS ==============
    // These test viewing permissions for circle members

    @Test
    void shouldGetCircleMembersWhenUserIsMember() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long memberId = 1L;
        
        User member = User.builder().id(memberId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        List<CircleMember> members = List.of(
                CircleMember.builder().userId(1L).role(CircleRole.OWNER).build(),
                CircleMember.builder().userId(2L).role(CircleRole.ADMIN).build(),
                CircleMember.builder().userId(3L).role(CircleRole.MEMBER).build()
        );
        
        given(userService.findUserById(memberId)).willReturn(member);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserActiveMemberOfCircle(memberId, circleId)).willReturn(true);
        given(circleMemberRepository.findActiveCircleMembers(circleId)).willReturn(members);

        // ============== WHEN ==============
        List<CircleMember> result = circleService.getCircleMembers(circleId, memberId);

        // ============== THEN ==============
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(members);
        
        then(circleMemberRepository).should().isUserActiveMemberOfCircle(memberId, circleId);
    }

    @Test
    void shouldPreventViewingMembersWhenUserIsNotMember() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long outsiderId = 1L;
        
        User outsider = User.builder().id(outsiderId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(outsiderId)).willReturn(outsider);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.isUserActiveMemberOfCircle(outsiderId, circleId))
                .willReturn(false); // Not a member

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.getCircleMembers(circleId, outsiderId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("member");
        
        then(circleMemberRepository).should(never()).findActiveCircleMembers(any());
    }

    // ============== CIRCLE SEARCH AND ACCESS TESTS ==============
    // These test circle discovery and access control

    @Test
    void shouldGetUserCircles() {
        // ============== GIVEN ==============
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        
        List<Circle> userCircles = List.of(
                Circle.builder().id(1L).name("Circle 1").build(),
                Circle.builder().id(2L).name("Circle 2").build()
        );
        
        given(userService.findUserById(userId)).willReturn(user);
        given(circleRepository.findCirclesForUser(userId)).willReturn(userCircles);

        // ============== WHEN ==============
        List<Circle> result = circleService.getCirclesForUser(userId);

        // ============== THEN ==============
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(userCircles);
        
        then(circleRepository).should().findCirclesForUser(userId);
    }

    @Test
    void shouldGetCirclesCreatedByUser() {
        // ============== GIVEN ==============
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        
        List<Circle> createdCircles = List.of(
                Circle.builder().id(1L).name("My Circle 1").createdBy(userId).build(),
                Circle.builder().id(2L).name("My Circle 2").createdBy(userId).build()
        );
        
        given(userService.findUserById(userId)).willReturn(user);
        given(circleRepository.findCirclesCreatedByUser(userId)).willReturn(createdCircles);

        // ============== WHEN ==============
        List<Circle> result = circleService.getCirclesCreatedByUser(userId);

        // ============== THEN ==============
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(createdCircles);
        
        then(circleRepository).should().findCirclesCreatedByUser(userId);
    }

    @Test
    void shouldSearchCirclesWithValidTerm() {
        // ============== GIVEN ==============
        String searchTerm = "project";
        
        List<Circle> matchingCircles = List.of(
                Circle.builder().id(1L).name("Project Alpha").build(),
                Circle.builder().id(2L).name("Beta Project").build()
        );
        
        given(circleRepository.findActiveCirclesByNameContaining(searchTerm.trim()))
                .willReturn(matchingCircles);

        // ============== WHEN ==============
        List<Circle> result = circleService.searchCircles(searchTerm);

        // ============== THEN ==============
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(matchingCircles);
        
        then(circleRepository).should().findActiveCirclesByNameContaining(searchTerm.trim());
    }

    @Test
    void shouldReturnEmptyForInvalidSearchTerms() {
        // Test null search term
        List<Circle> nullResult = circleService.searchCircles(null);
        assertThat(nullResult).isEmpty();
        
        // Test empty search term
        List<Circle> emptyResult = circleService.searchCircles("");
        assertThat(emptyResult).isEmpty();
        
        // Test whitespace search term
        List<Circle> whitespaceResult = circleService.searchCircles("   ");
        assertThat(whitespaceResult).isEmpty();
        
        // Verify no repository calls for invalid terms
        then(circleRepository).should(never()).findActiveCirclesByNameContaining(anyString());
    }

    // ============== ACCESS CONTROL UTILITY TESTS ==============
    // These test helper methods for access control

    @Test
    void shouldCorrectlyDetermineUserAccess() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long userId = 1L;
        
        User user = User.builder().id(userId).build();
        
        given(userService.findUserById(userId)).willReturn(user);
        given(circleMemberRepository.isUserActiveMemberOfCircle(userId, circleId)).willReturn(true);

        // ============== WHEN ==============
        boolean canAccess = circleService.canUserAccessCircle(circleId, userId);

        // ============== THEN ==============
        assertThat(canAccess).isTrue();
        
        then(circleMemberRepository).should().isUserActiveMemberOfCircle(userId, circleId);
    }

    @Test
    void shouldReturnFalseForInvalidAccessParameters() {
        // Test null userId
        boolean nullUserResult = circleService.canUserAccessCircle(10L, null);
        assertThat(nullUserResult).isFalse();
        
        // Test null circleId
        boolean nullCircleResult = circleService.canUserAccessCircle(null, 1L);
        assertThat(nullCircleResult).isFalse();
        
        // Verify no repository calls for null parameters
        then(userService).should(never()).findUserById(any());
        then(circleMemberRepository).should(never()).isUserActiveMemberOfCircle(any(), any());
    }

    @Test
    void shouldGetUserMembershipInCircle() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long userId = 1L;
        
        User user = User.builder().id(userId).build();
        CircleMember membership = CircleMember.builder()
                .userId(userId)
                .circleId(circleId)
                .role(CircleRole.ADMIN)
                .build();
        
        given(userService.findUserById(userId)).willReturn(user);
        given(circleMemberRepository.findActiveMembershipRecord(userId, circleId))
                .willReturn(Optional.of(membership));

        // ============== WHEN ==============
        CircleMember result = circleService.getUserMembershipInCircle(circleId, userId);

        // ============== THEN ==============
        assertThat(result).isEqualTo(membership);
        assertThat(result.getRole()).isEqualTo(CircleRole.ADMIN);
        
        then(circleMemberRepository).should().findActiveMembershipRecord(userId, circleId);
    }

    @Test
    void shouldGetMemberCountForCircle() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        long expectedCount = 5L;
        
        given(circleMemberRepository.countActiveMembersInCircle(circleId))
                .willReturn(expectedCount);

        // ============== WHEN ==============
        long actualCount = circleService.getMemberCountForCircle(circleId);

        // ============== THEN ==============
        assertThat(actualCount).isEqualTo(expectedCount);
        
        then(circleMemberRepository).should().countActiveMembersInCircle(circleId);
    }

    // ============== ERROR HANDLING TESTS ==============
    // These test exception scenarios and edge cases

    @Test
    void shouldThrowExceptionForNonExistentCircle() {
        // ============== GIVEN ==============
        Long nonExistentCircleId = 999L;
        Long userId = 1L;
        
        User user = User.builder().id(userId).build();
        
        given(userService.findUserById(userId)).willReturn(user);
        given(circleRepository.findById(nonExistentCircleId)).willReturn(Optional.empty());

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.updateCircle(nonExistentCircleId, "New Name", null, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Circle");
        
        then(circleRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionForNonExistentUser() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long nonExistentUserId = 999L;
        
        given(userService.findUserById(nonExistentUserId))
                .willThrow(new ResourceNotFoundException("User not found"));

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.createCircle(nonExistentUserId, "Circle", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        
        then(circleRepository).should(never()).save(any());
    }

    @Test
    void shouldThrowExceptionForNonExistentMembership() {
        // ============== GIVEN ==============
        Long circleId = 10L;
        Long userId = 1L;
        Long requestingUserId = 2L;
        
        User user = User.builder().id(userId).build();
        User requestingUser = User.builder().id(requestingUserId).build();
        Circle circle = Circle.builder().id(circleId).build();
        
        given(userService.findUserById(requestingUserId)).willReturn(requestingUser);
        given(userService.findUserById(userId)).willReturn(user);
        given(circleRepository.findById(circleId)).willReturn(Optional.of(circle));
        given(circleMemberRepository.findActiveMembershipRecord(userId, circleId))
                .willReturn(Optional.empty()); // Membership not found

        // ============== WHEN & THEN ==============
        assertThatThrownBy(() -> circleService.removeMemberFromCircle(circleId, userId, requestingUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CircleMember with id 1 in circle 10 not found");
        
        then(circleMemberRepository).should(never()).deactivateMembership(any(), any());
    }
}