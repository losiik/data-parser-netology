package _gis.company_search.service;

import org.springframework.stereotype.Service;
import java.util.Optional;
import reactor.core.publisher.Mono;

import _gis.company_search.entity.UserEntity;
import _gis.company_search.repository.UserRepository;
import _gis.company_search.exception.InvalidPasswordException;
import _gis.company_search.exception.UserNotFoundException;

@Service
public class UserService {

    private final HashService hashService;
    private final UserRepository userRepository;

    public UserService(HashService hashService, UserRepository userRepository) {
        this.hashService = hashService;
        this.userRepository = userRepository;
    }

    public Mono<UserEntity> createUser(UserEntity user) {
        return Mono.fromCallable(() -> {
            String hashPassword = hashService.hashString(user.getPassword());
            user.setPassword(hashPassword);
            return userRepository.save(user);
        });
    }

    public Mono<UserEntity> login(String email, String password) {
        return Mono.fromCallable(() -> {
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);

            UserEntity user = userOpt.orElseThrow(
                    UserNotFoundException::new
            );

            String hashPassword = hashService.hashString(password);

            if (!user.getPassword().equals(hashPassword)) {
                throw new InvalidPasswordException();
            }

            return user;
        });
    }
}
