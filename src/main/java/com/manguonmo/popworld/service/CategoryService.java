package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories();
    Optional<Category> getCategoryBySlug(String slug);
}
