package com.friendavailability.domain.service;

import com.friendavailability.base.BaseUnitTest;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.DuplicateResourceException;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

//All tests passed
class UserServiceTest extends BaseUnitTest{

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldFindUserById() {
        Long userId = 1L;
        User expectedUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(expectedUser));

        User actualUser = userService.findUserById(userId);

        assertThat(actualUser).isEqualTo(expectedUser);
        then(userRepository).should().findById(userId);

    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFound(){
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with id 999 not found");

        then(userRepository).should().findById(userId);
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        String email = "test@example.com";
        User expectedUser = User.builder()
                .id(1L)
                .email(email)
                .name("Test User")
                .build();
        
        given(userRepository.findByEmail(email)).willReturn(Optional.of(expectedUser));

        // When
        User actualUser = userService.findUserByEmail(email);

        // Then
        assertThat(actualUser).isEqualTo(expectedUser);
        then(userRepository).should().findByEmail(email);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundByEmail() {
        // Given
        String email = "nonexistent@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findUserByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with id nonexistent@example.com not found");
        
        then(userRepository).should().findByEmail(email);
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        String email = "newuser@example.com";
        String name = "New User";
        String password = "securePassword";
        
        given(userRepository.existsByEmail(email)).willReturn(false);
        
        User savedUser = User.builder()
                .id(1L)
                .email(email)
                .name(name)
                .build();
        
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // When
        User createdUser = userService.createUserWithPassword(name, email, password);

        // Then
        assertThat(createdUser).isEqualTo(savedUser);
        then(userRepository).should().existsByEmail(email);
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenEmailAlreadyExists() {
        // Given
        String email = "existing@example.com";
        String name = "Test User";
        String password = "password";
        
        given(userRepository.existsByEmail(email)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUserWithPassword(name, email, password))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User with email 'existing@example.com' already exists");
        
        then(userRepository).should().existsByEmail(email);
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Given
        Long userId = 1L;
        String newName = "Updated Name";
        String newEmail = "updated@example.com";
        
        User existingUser = User.builder()
                .id(userId)
                .email("old@example.com")
                .name("Old Name")
                .build();
        
        User updatedUser = User.builder()
                .id(userId)
                .email(newEmail)
                .name(newName)
                .build();
        
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(userRepository.existsByEmail(newEmail)).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(updatedUser);

        // When
        User result = userService.updateUser(userId, newName, newEmail);

        // Then
        assertThat(result).isEqualTo(updatedUser);
        then(userRepository).should().findById(userId);
        then(userRepository).should().existsByEmail(newEmail);
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenUpdatingWithExistingEmail() {
        // Given
        Long userId = 1L;
        String newName = "Updated Name";
        String existingEmail = "existing@example.com";
        
        User existingUser = User.builder()
                .id(userId)
                .email("old@example.com")
                .name("Old Name")
                .build();
        
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(userRepository.existsByEmail(existingEmail)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, newName, existingEmail))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User with email 'existing@example.com' already exists");
        
        then(userRepository).should().findById(userId);
        then(userRepository).should().existsByEmail(existingEmail);
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        // Given
        Long userId = 1L;
        User existingUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .build();
        
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));

        // When
        userService.deleteUserById(userId);

        // Then
        then(userRepository).should().findById(userId);
        then(userRepository).should().deleteById(userId);
    }

    @Test
    void shouldGetAllUsers() {
        // Given
        User user1 = User.builder().id(1L).email("user1@example.com").name("User One").build();
        User user2 = User.builder().id(2L).email("user2@example.com").name("User Two").build();
        
        given(userRepository.findAll()).willReturn(List.of(user1, user2));

        // When
        List<User> users = userService.findAllUsers();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).contains(user1, user2);
        then(userRepository).should().findAll();
    }


}
