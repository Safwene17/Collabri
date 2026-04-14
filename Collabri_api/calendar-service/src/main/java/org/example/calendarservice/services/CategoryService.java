package org.example.calendarservice.services;

import lombok.RequiredArgsConstructor;
import org.example.calendarservice.dto.CategoryRequest;
import org.example.calendarservice.dto.CategoryResponse;
import org.example.calendarservice.entites.Category;
import org.example.calendarservice.exceptions.CustomException;
import org.example.calendarservice.mappers.CategoryMapper;
import org.example.calendarservice.repositories.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String normalizedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CustomException("Category name already exists", HttpStatus.CONFLICT);
        }

        Category saved = categoryRepository.save(categoryMapper.toCategory(request));
        return categoryMapper.fromCategory(saved);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(String search, Pageable pageable) {
        Page<Category> page = (search == null || search.isBlank())
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByNameContainingIgnoreCase(search.trim(), pageable);

        return page.map(categoryMapper::fromCategory);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));
        return categoryMapper.fromCategory(category);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));

        String normalizedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new CustomException("Category name already exists", HttpStatus.CONFLICT);
        }

        categoryMapper.update(category, request);
        Category updated = categoryRepository.save(category);
        return categoryMapper.fromCategory(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException("Category not found", HttpStatus.NOT_FOUND));

        if (category.getCalendars() != null && !category.getCalendars().isEmpty()) {
            throw new CustomException("Cannot delete category assigned to calendars", HttpStatus.CONFLICT);
        }

        categoryRepository.delete(category);
    }
}
