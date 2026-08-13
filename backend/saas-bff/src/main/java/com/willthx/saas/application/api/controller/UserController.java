package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.user.CreateUserRq;
import com.willthx.saas.application.api.dto.user.UpdateStationsRq;
import com.willthx.saas.application.api.dto.user.UpdateUserRq;
import com.willthx.saas.application.api.dto.user.UserRs;
import com.willthx.saas.application.usecase.user.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUsersUseCase       getUsersUseCase;
    private final GetUserUseCase        getUserUseCase;
    private final CreateUserUseCase     createUserUseCase;
    private final UpdateUserUseCase     updateUserUseCase;
    private final DisableUserUseCase    disableUserUseCase;
    private final GetStationsUseCase    getStationsUseCase;
    private final UpdateStationsUseCase updateStationsUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserRs>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.success(getUsersUseCase.execute()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRs>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(getUserUseCase.execute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserRs>> createUser(@Valid @RequestBody CreateUserRq rq) {
        return ResponseEntity.ok(ApiResponse.success(createUserUseCase.execute(rq)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRs>> updateUser(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateUserRq rq) {
        return ResponseEntity.ok(ApiResponse.success(updateUserUseCase.execute(id, rq)));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
        disableUserUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/stations")
    public ResponseEntity<ApiResponse<List<String>>> getStations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(getStationsUseCase.execute(id)));
    }

    @PutMapping("/{id}/stations")
    public ResponseEntity<ApiResponse<Void>> updateStations(@PathVariable Long id,
                                                            @RequestBody UpdateStationsRq rq) {
        updateStationsUseCase.execute(id, rq);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
