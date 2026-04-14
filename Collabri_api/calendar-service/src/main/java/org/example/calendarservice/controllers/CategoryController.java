package org.example.calendarservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.ApiResponse;
import org.example.calendarservice.dto.CategoryRequest;
import org.example.calendarservice.dto.CategoryResponse;
import org.example.calendarservice.services.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCategory(@RequestBody @Valid CategoryRequest request) {
        categoryService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.ok("Category created successfully", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Category retrieved successfully", categoryService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getCategories(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        Page<CategoryResponse> result = categoryService.getAll(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Categories retrieved successfully", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @PathVariable UUID id,
            @RequestBody @Valid CategoryRequest request
    ) {
        categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.status(204).body(ApiResponse.ok("Category deleted successfully", null));
    }
}
