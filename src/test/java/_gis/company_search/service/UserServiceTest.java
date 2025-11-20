package _gis.company_search.service;

import _gis.company_search.entity.UserEntity;
import _gis.company_search.exception.InvalidPasswordException;
import _gis.company_search.exception.UserNotFoundException;
import _gis.company_search.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class UserServiceTest {
    private HashService hashService;
    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setup() {
        hashService = mock(HashService.class);
        userRepository = mock(UserRepository.class);
        userService = new UserService(hashService, userRepository);
    }

    @Test
    void createUser_ShouldHashPassword_AndSaveUser() throws Exception {
        UserEntity user = new UserEntity();
        user.setEmail("test@mail.com");
        user.setPassword("rawpass");

        when(hashService.hashString("rawpass"))
                .thenReturn("hashedPass123");

        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<UserEntity> result = userService.createUser(user);

        StepVerifier.create(result)
                .assertNext(savedUser -> {
                    try {
                        verify(hashService, times(1)).hashString("rawpass");
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    verify(userRepository, times(1)).save(savedUser);

                    assert savedUser.getPassword().equals("hashedPass123");
                })
                .verifyComplete();
    }

    @Test
    void login_ShouldReturnUser_WhenCredentialsCorrect() throws Exception {
        UserEntity stored = new UserEntity();
        stored.setEmail("test@mail.com");
        stored.setPassword("hashedPass123");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(stored));

        when(hashService.hashString("rawPass"))
                .thenReturn("hashedPass123");

        Mono<UserEntity> result = userService.login("test@mail.com", "rawPass");

        StepVerifier.create(result)
                .expectNext(stored)
                .verifyComplete();

        verify(userRepository, times(1)).findByEmail("test@mail.com");
        verify(hashService, times(1)).hashString("rawPass");
    }

    @Test
    void login_ShouldThrow_WhenUserNotFound() throws Exception {
        when(userRepository.findByEmail("unknown@mail.com"))
                .thenReturn(Optional.empty());

        Mono<UserEntity> result = userService.login("unknown@mail.com", "pass");

        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepository, times(1)).findByEmail("unknown@mail.com");
        verify(hashService, never()).hashString(anyString());
    }

    @Test
    void login_ShouldThrow_WhenPasswordIncorrect() throws Exception {
        UserEntity stored = new UserEntity();
        stored.setEmail("test@mail.com");
        stored.setPassword("correctHash");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(stored));

        when(hashService.hashString("wrongPass"))
                .thenReturn("anotherHash");

        Mono<UserEntity> result = userService.login("test@mail.com", "wrongPass");

        StepVerifier.create(result)
                .expectError(InvalidPasswordException.class)
                .verify();

        verify(userRepository, times(1)).findByEmail("test@mail.com");
        verify(hashService, times(1)).hashString("wrongPass");
    }

    @Test
    void createUser_ShouldPropagateException_WhenHashFails() throws Exception {
        UserEntity user = new UserEntity();
        user.setEmail("x@mail.com");
        user.setPassword("123");

        when(hashService.hashString("123"))
                .thenThrow(new NoSuchAlgorithmException());

        Mono<UserEntity> result = userService.createUser(user);

        StepVerifier.create(result)
                .expectError(NoSuchAlgorithmException.class)
                .verify();

        verify(userRepository, never()).save(any());
    }
}
