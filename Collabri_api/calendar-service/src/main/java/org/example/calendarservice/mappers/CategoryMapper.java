package org.example.calendarservice.mappers;

import org.example.calendarservice.dto.CategoryRequest;
import org.example.calendarservice.dto.CategoryResponse;
import org.example.calendarservice.entites.Category;
import org.springframework.stereotype.Service;

@Service
public class CategoryMapper {


    public Category toCategory(CategoryRequest request) {
        return Category.builder()
                .name(request.name().trim())
                .description(request.description().trim())
                .build();
    }

    public CategoryResponse fromCategory(Category category) {
        long calendarsCount = category.getCalendars() == null ? 0 : category.getCalendars().size();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                calendarsCount
        );
    }

    public void update(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description().trim());
    }
}
