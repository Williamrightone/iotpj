package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.DeactivateResultRs;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import com.willthx.saas.application.api.dto.component.CreateComponentRq;
import com.willthx.saas.application.api.dto.machine.CreateMachineRq;
import com.willthx.saas.application.api.dto.machine.MachineRs;
import com.willthx.saas.application.api.dto.station.*;
import com.willthx.saas.application.usecase.component.CreateStationComponentUseCase;
import com.willthx.saas.application.usecase.machine.CreateMachineUseCase;
import com.willthx.saas.application.usecase.station.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final GetStationListUseCase        getStationListUseCase;
    private final CreateStationUseCase         createStationUseCase;
    private final UpdateStationUseCase         updateStationUseCase;
    private final ReorderStationsUseCase       reorderStationsUseCase;
    private final DeactivateStationUseCase     deactivateStationUseCase;
    private final ActivateStationUseCase       activateStationUseCase;
    private final GetStationDetailUseCase      getStationDetailUseCase;
    private final CreateMachineUseCase         createMachineUseCase;
    private final CreateStationComponentUseCase createStationComponentUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StationSummaryRs>>> listStations() {
        return ResponseEntity.ok(ApiResponse.success(getStationListUseCase.execute()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StationRs>> createStation(@Valid @RequestBody CreateStationRq rq) {
        return ResponseEntity.ok(ApiResponse.success(createStationUseCase.execute(rq)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StationRs>> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStationRq rq) {
        return ResponseEntity.ok(ApiResponse.success(updateStationUseCase.execute(id, rq)));
    }

    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderStations(@Valid @RequestBody ReorderRq rq) {
        reorderStationsUseCase.execute(rq);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<DeactivateResultRs>> deactivateStation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(deactivateStationUseCase.execute(id)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<StationRs>> activateStation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(activateStationUseCase.execute(id)));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<StationDetailRs>> getStationDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(getStationDetailUseCase.execute(id)));
    }

    @PostMapping("/{stationId}/machines")
    public ResponseEntity<ApiResponse<MachineRs>> createMachine(
            @PathVariable Long stationId,
            @Valid @RequestBody CreateMachineRq rq) {
        return ResponseEntity.ok(ApiResponse.success(createMachineUseCase.execute(stationId, rq)));
    }

    @PostMapping("/{stationId}/components")
    public ResponseEntity<ApiResponse<ComponentRs>> createStationComponent(
            @PathVariable Long stationId,
            @Valid @RequestBody CreateComponentRq rq) {
        return ResponseEntity.ok(ApiResponse.success(createStationComponentUseCase.execute(stationId, rq)));
    }
}
