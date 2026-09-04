package com.sih.supplychain.controller;

import com.sih.supplychain.dto.material.MaterialCreateRequest;
import com.sih.supplychain.dto.material.MaterialResponse;
import com.sih.supplychain.dto.material.MaterialUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialResponse> listMaterials(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String criticality
    ) {
        if (hasText(code)) {
            return List.of(OperationalMapper.toMaterialResponse(this.materialService.getMaterialByCode(code)));
        }
        if (hasText(status)) {
            return this.materialService.getMaterialsByStatus(status)
                    .stream()
                    .map(OperationalMapper::toMaterialResponse)
                    .toList();
        }
        if (hasText(criticality)) {
            return this.materialService.getMaterialsByCriticality(criticality)
                    .stream()
                    .map(OperationalMapper::toMaterialResponse)
                    .toList();
        }
        return this.materialService.getAllMaterials()
                .stream()
                .map(OperationalMapper::toMaterialResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public MaterialResponse getMaterial(@PathVariable Long id) {
        return OperationalMapper.toMaterialResponse(this.materialService.getMaterialById(id));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> createMaterial(@Valid @RequestBody MaterialCreateRequest request) {
        MaterialResponse response = OperationalMapper.toMaterialResponse(
                this.materialService.createMaterial(OperationalMapper.toMaterial(request))
        );
        return ResponseEntity.created(URI.create("/api/materials/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public MaterialResponse updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody MaterialUpdateRequest request
    ) {
        return OperationalMapper.toMaterialResponse(
                this.materialService.updateMaterial(id, OperationalMapper.toMaterial(request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        this.materialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
