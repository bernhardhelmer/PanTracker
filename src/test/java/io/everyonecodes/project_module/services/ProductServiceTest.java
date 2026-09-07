package io.everyonecodes.project_module.services;

import io.everyonecodes.project_module.dtos.requests.ProductRequest;
import io.everyonecodes.project_module.dtos.responses.ProductResponse;
import io.everyonecodes.project_module.exceptions.ResourceNotFoundException;
import io.everyonecodes.project_module.models.Category;
import io.everyonecodes.project_module.models.Product;
import io.everyonecodes.project_module.models.User;
import io.everyonecodes.project_module.repositories.CategoryRepository;
import io.everyonecodes.project_module.repositories.ProductRepository;
import io.everyonecodes.project_module.repositories.UsageLogRepository;
import io.everyonecodes.project_module.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UsageLogRepository usageLogRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSuccess() {
        // inputs and mock behaviors
        Long userId = 1L;
        Long categoryId = 5L;

        ProductRequest request = ProductRequest.builder()
                .name("Nude Brown Honey")
                .brand("MAC")
                .categoryId(categoryId)
                .openingDate(LocalDate.now())
                .periodAfterOpeningMonths(12)
                .startingWeightGrams(BigDecimal.valueOf(67.8))
                .build();

        User user = User.builder().id(userId).username("Felix").build();
        Category category = Category.builder().id(categoryId).name("Lipstick").build();

        Product savedProduct = Product.builder()
                .id(1L)
                .user(user)
                .category(category)
                .name(request.getName())
                .brand(request.getBrand())
                .openingDate(request.getOpeningDate())
                .periodAfterOpeningMonths(request.getPeriodAfterOpeningMonths())
                .startingWeightGrams(request.getStartingWeightGrams())
                .currentWeightGrams(request.getStartingWeightGrams())
                .isFinished(false)
                .build();

        // behavioral instructions
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(usageLogRepository.countByProductId(anyLong())).thenReturn(0);

        ProductResponse response = productService.createProduct(userId, request);

        // check results
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Lipstick");
        assertThat(response.getCurrentWeightGrams()).isEqualTo(BigDecimal.valueOf(67.8));
        assertThat(response.isFinished()).isFalse();

        // check execution
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProductThrowsExceptionWhenUserNotFound() {
        Long userId = 67L;
        ProductRequest request = ProductRequest.builder().categoryId(1L).build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with ID 67 not found");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getActiveCollectionSuccess() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("Jakob").build();
        Category category = Category.builder().id(5L).name("Lipstick").build();

        Product activeProduct = Product.builder()
                .id(1L)
                .user(user)
                .category(category)
                .name("Nude Brown Honey")
                .brand("MAC")
                .openingDate(LocalDate.of(2026, 1, 15))
                .periodAfterOpeningMonths(12)
                .isFinished(false)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(productRepository.findByUserIdAndIsFinishedOrderByCategoryNameAscOpeningDateAsc(userId, false))
                .thenReturn(java.util.List.of(activeProduct));
        when(usageLogRepository.countByProductId(anyLong())).thenReturn(0);

        List<ProductResponse> responseList = productService.getActiveCollection(userId);

        assertThat(responseList).isNotEmpty().hasSize(1);
        ProductResponse response = responseList.getFirst();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Lipstick");
        assertThat(response.isFinished()).isFalse();

        verify(productRepository, times(1)).findByUserIdAndIsFinishedOrderByCategoryNameAscOpeningDateAsc(userId, false);
    }

    @Test
    void getEmptiesCollectionSuccess() {
        Long userId = 2L;
        User user = User.builder().id(userId).username("Jakob").build();
        Category category = Category.builder().id(5L).name("Lipstick").build();

        Product finishedProduct = Product.builder()
                .id(2L)
                .user(user)
                .category(category)
                .name("Finished Lipstick")
                .openingDate(LocalDate.of(2026, 1, 15))
                .periodAfterOpeningMonths(12)
                .isFinished(true)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(productRepository.findByUserIdAndIsFinishedOrderByCategoryNameAscOpeningDateAsc(userId, true))
                .thenReturn(java.util.List.of(finishedProduct));
        when(usageLogRepository.countByProductId(anyLong())).thenReturn(0);

        java.util.List<ProductResponse> responseList = productService.getEmptiesCollection(userId);

        assertThat(responseList).isNotEmpty().hasSize(1);
        ProductResponse response = responseList.getFirst();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.isFinished()).isTrue();

        verify(productRepository, times(1)).findByUserIdAndIsFinishedOrderByCategoryNameAscOpeningDateAsc(userId, true);
    }

    @Test
    void getProductById_Success() {
        Long productId = 10L;
        Category category = Category.builder().id(5L).name("Mascara").build();
        User user = User.builder().id(1L).username("David").build();

        Product product = Product.builder()
                .id(productId)
                .user(user)
                .category(category)
                .name("Ultra Black Mascara")
                .brand("NARS")
                .openingDate(LocalDate.of(2026, 1, 15))
                .periodAfterOpeningMonths(12)
                .isFinished(false)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        when(usageLogRepository.countByProductId(productId)).thenReturn(14);

        ProductResponse response = productService.getProductById(productId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(productId);
        assertThat(response.getName()).isEqualTo("Ultra Black Mascara");
        assertThat(response.getCategoryName()).isEqualTo("Mascara");
        assertThat(response.getTotalUses()).isEqualTo(14);
    }

    @Test
    void getProductById_ThrowsException_WhenProductNotFound() {
        Long productId = 99L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with ID 99 not found");
    }

    @Test
    void updateProductSuccessNoCategoryChange() {
        Long productId = 10L;
        Long categoryId = 5L;
        Category category = Category.builder().id(categoryId).name("Lipstick").build();
        User user = User.builder().id(1L).username("Jakob").build();

        ProductRequest request = ProductRequest.builder()
                .name("Updated Name")
                .brand("Updated Brand")
                .categoryId(categoryId) // Same category ID
                .openingDate(LocalDate.now())
                .periodAfterOpeningMonths(24)
                .startingWeightGrams(BigDecimal.valueOf(12.2))
                .rating(9)
                .build();

        Product existingProduct = Product.builder()
                .id(productId)
                .user(user)
                .category(category)
                .name("Old Name")
                .brand("Old Brand")
                .build();

        Product updatedProduct = Product.builder()
                .id(productId)
                .user(user)
                .category(category)
                .name(request.getName())
                .brand(request.getBrand())
                .openingDate(request.getOpeningDate())
                .periodAfterOpeningMonths(request.getPeriodAfterOpeningMonths())
                .startingWeightGrams(request.getStartingWeightGrams())
                .rating(request.getRating())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(usageLogRepository.countByProductId(anyLong())).thenReturn(0);

        ProductResponse response = productService.updateProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(productId);
        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getBrand()).isEqualTo("Updated Brand");
        assertThat(response.getRating()).isEqualTo(9);

        // verify we never queried categoryRepository because the category ID did not change
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProductSuccessWithCategoryChange() {
        Long productId = 10L;
        Long oldCategoryId = 5L;
        Long newCategoryId = 6L;

        Category oldCategory = Category.builder().id(oldCategoryId).name("Lipstick").build();
        Category newCategory = Category.builder().id(newCategoryId).name("Lip Gloss").build();
        User user = User.builder().id(1L).username("Jakob").build();

        ProductRequest request = ProductRequest.builder()
                .name("Lip Gloss Product")
                .categoryId(newCategoryId) // Changed category ID
                .openingDate(LocalDate.now())
                .periodAfterOpeningMonths(12)
                .build();

        Product existingProduct = Product.builder()
                .id(productId)
                .user(user)
                .category(oldCategory)
                .name("Old Name")
                .build();

        Product updatedProduct = Product.builder()
                .id(productId)
                .user(user)
                .category(newCategory)
                .name(request.getName())
                .openingDate(request.getOpeningDate())
                .periodAfterOpeningMonths(request.getPeriodAfterOpeningMonths())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(usageLogRepository.countByProductId(anyLong())).thenReturn(0);

        ProductResponse response = productService.updateProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCategoryName()).isEqualTo("Lip Gloss");

        // verify we did query the category repository because the ID changed
        verify(categoryRepository, times(1)).findById(newCategoryId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteProductSuccess() {
        Long productId = 10L;
        Product product = Product.builder().id(productId).name("Item to Delete").build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        verify(productRepository, times(1)).delete(product);
    }

    @Test
    void deleteProductThrowsExceptionWhenProductNotFound() {
        Long productId = 67L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with ID 67 not found");

        verify(productRepository, never()).delete(any(Product.class));
    }
}
