package com.willthx.saas.adapter.feign;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.DeactivateResultRs;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import com.willthx.saas.application.api.dto.component.CreateComponentRq;
import com.willthx.saas.application.api.dto.component.UpdateComponentRq;
import com.willthx.saas.application.api.dto.machine.CopyMachineRq;
import com.willthx.saas.application.api.dto.machine.CreateMachineRq;
import com.willthx.saas.application.api.dto.machine.MachineRs;
import com.willthx.saas.application.api.dto.machine.UpdateMachineRq;
import com.willthx.saas.application.api.dto.station.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "iotcore-service", url = "${iotcore.service.url:http://localhost:8082}")
public interface IotCoreFeignClient {

    // ── Stations ─────────────────────────────────────────────────────────────

    @GetMapping("/internal/stations")
    ApiResponse<List<StationSummaryRs>> listStations(@RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/stations")
    ApiResponse<StationRs> createStation(@RequestHeader("X-Tenant-Id") Long tenantId,
                                         @RequestBody CreateStationRq rq);

    @PutMapping("/internal/stations/{id}")
    ApiResponse<StationRs> updateStation(@PathVariable("id") Long id,
                                         @RequestHeader("X-Tenant-Id") Long tenantId,
                                         @RequestBody UpdateStationRq rq);

    @PostMapping("/internal/stations/reorder")
    ApiResponse<Void> reorderStations(@RequestHeader("X-Tenant-Id") Long tenantId,
                                      @RequestBody ReorderRq rq);

    @PostMapping("/internal/stations/{id}/deactivate")
    ApiResponse<DeactivateResultRs> deactivateStation(@PathVariable("id") Long id,
                                                       @RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/stations/{id}/activate")
    ApiResponse<StationRs> activateStation(@PathVariable("id") Long id,
                                           @RequestHeader("X-Tenant-Id") Long tenantId);

    @GetMapping("/internal/stations/{id}/detail")
    ApiResponse<StationDetailRs> getStationDetail(@PathVariable("id") Long id,
                                                   @RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/stations/{stationId}/machines")
    ApiResponse<MachineRs> createMachine(@PathVariable("stationId") Long stationId,
                                         @RequestHeader("X-Tenant-Id") Long tenantId,
                                         @RequestBody CreateMachineRq rq);

    @PostMapping("/internal/stations/{stationId}/components")
    ApiResponse<ComponentRs> createStationComponent(@PathVariable("stationId") Long stationId,
                                                     @RequestHeader("X-Tenant-Id") Long tenantId,
                                                     @RequestBody CreateComponentRq rq);

    // ── Machines ─────────────────────────────────────────────────────────────

    @PutMapping("/internal/machines/{id}")
    ApiResponse<MachineRs> updateMachine(@PathVariable("id") Long id,
                                         @RequestHeader("X-Tenant-Id") Long tenantId,
                                         @RequestBody UpdateMachineRq rq);

    @PostMapping("/internal/machines/{id}/deactivate")
    ApiResponse<DeactivateResultRs> deactivateMachine(@PathVariable("id") Long id,
                                                       @RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/machines/{id}/activate")
    ApiResponse<MachineRs> activateMachine(@PathVariable("id") Long id,
                                           @RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/machines/{id}/copy")
    ApiResponse<MachineRs> copyMachine(@PathVariable("id") Long id,
                                       @RequestHeader("X-Tenant-Id") Long tenantId,
                                       @RequestBody CopyMachineRq rq);

    @PostMapping("/internal/machines/{machineId}/components")
    ApiResponse<ComponentRs> createMachineComponent(@PathVariable("machineId") Long machineId,
                                                     @RequestHeader("X-Tenant-Id") Long tenantId,
                                                     @RequestBody CreateComponentRq rq);

    // ── Components ───────────────────────────────────────────────────────────

    @PutMapping("/internal/components/{id}")
    ApiResponse<ComponentRs> updateComponent(@PathVariable("id") Long id,
                                             @RequestHeader("X-Tenant-Id") Long tenantId,
                                             @RequestBody UpdateComponentRq rq);

    @PostMapping("/internal/components/{id}/deactivate")
    ApiResponse<Void> deactivateComponent(@PathVariable("id") Long id,
                                          @RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/components/{id}/activate")
    ApiResponse<ComponentRs> activateComponent(@PathVariable("id") Long id,
                                               @RequestHeader("X-Tenant-Id") Long tenantId);
}
