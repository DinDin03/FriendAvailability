package com.friendavailability.domain.service;

import com.friendavailability.base.BaseUnitTest;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.DuplicateResourceException;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class UserServiceTest extends BaseUnitTest{

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldFindUserById(){
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
}
