package _gis.company_search.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import _gis.company_search.dto.LoginRequest;
import _gis.company_search.dto.UserRequest;
import _gis.company_search.dto.UserResponse;
import _gis.company_search.entity.UserEntity;
import _gis.company_search.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/")
    public Mono<UserResponse> createUser(@RequestBody UserRequest request) {
        UserEntity userEntity = new UserEntity();
        userEntity.setName(request.getName());
        userEntity.setPassword(request.getPassword());
        userEntity.setEmail(request.getEmail());

        return userService.createUser(userEntity).map(UserResponse::new);
    }

    @PostMapping("/login/")
    public Mono<UserResponse> loginUser(@RequestBody LoginRequest request) {
        return userService.login(request.getEmail(), request.getPassword()).map(UserResponse::new);
    }
}