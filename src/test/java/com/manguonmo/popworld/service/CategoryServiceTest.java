package com.manguonmo.popworld.service;

import com.manguonmo.popworld.service.impl.CategoryServiceImpl;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("Lấy toàn bộ danh mục thành công")
    void test_GetAllCategories_Success() {
        Category c1 = Category.builder().id(1L).name("Blind Box").slug("blind-box").build();
        Category c2 = Category.builder().id(2L).name("Mega").slug("mega-collection").build();

        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Category> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Blind Box", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Lấy danh mục theo slug thành công")
    void test_GetCategoryBySlug_Success() {
        Category c = Category.builder().id(1L).name("Blind Box").slug("blind-box").build();
        when(categoryRepository.findBySlug("blind-box")).thenReturn(Optional.of(c));

        Optional<Category> result = categoryService.getCategoryBySlug("blind-box");

        assertTrue(result.isPresent());
        assertEquals("Blind Box", result.get().getName());
        verify(categoryRepository, times(1)).findBySlug("blind-box");
    }
}
