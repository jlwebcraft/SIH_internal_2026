package com.sih.supplychain.service;

import com.sih.supplychain.domain.Product;
import com.sih.supplychain.domain.ProductionOrder;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.ProductRepository;
import com.sih.supplychain.repository.ProductionOrderRepository;
import com.sih.supplychain.repository.UserRepository;
import com.sih.supplychain.service.ProductionOrderService.CreateProductionOrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionOrderServiceTests {

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    private ProductionOrderService productionOrderService;

    @BeforeEach
    void setUp() {
        this.productionOrderService = new ProductionOrderService(
                this.productionOrderRepository,
                this.productRepository,
                this.userRepository
        );
    }

    @Test
    void createProductionOrder_success() {
        Product product = new Product("PROD-001", "Motor");
        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.productionOrderRepository.existsByProductionNumber("PR-100")).thenReturn(false);
        when(this.productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                "PR-100",
                1L,
                new BigDecimal("50"),
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                null,
                null,
                "HIGH",
                null
        );

        ProductionOrder created = this.productionOrderService.createProductionOrder(command);

        assertThat(created).isNotNull();
        assertThat(created.getProductionNumber()).isEqualTo("PR-100");
        assertThat(created.getProduct()).isEqualTo(product);
        assertThat(created.getQuantity()).isEqualTo(new BigDecimal("50"));
        assertThat(created.getStatus()).isEqualTo(ProductionOrderService.STATUS_PLANNED);
        assertThat(created.getPriority()).isEqualTo("HIGH");
    }

    @Test
    void createProductionOrder_missingProduct_throwsNotFound() {
        when(this.productRepository.findById(99L)).thenReturn(Optional.empty());

        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                "PR-100",
                99L,
                new BigDecimal("50"),
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                null,
                null,
                "HIGH",
                null
        );

        assertThatThrownBy(() -> this.productionOrderService.createProductionOrder(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");

        verify(this.productionOrderRepository, never()).save(any());
    }

    @Test
    void createProductionOrder_duplicateNumber_throwsDuplicate() {
        Product product = new Product("PROD-001", "Motor");
        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.productionOrderRepository.existsByProductionNumber("PR-100")).thenReturn(true);

        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                "PR-100",
                1L,
                new BigDecimal("50"),
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                null,
                null,
                "HIGH",
                null
        );

        assertThatThrownBy(() -> this.productionOrderService.createProductionOrder(command))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Production order number already exists");

        verify(this.productionOrderRepository, never()).save(any());
    }

    @Test
    void createProductionOrder_invalidQuantity_throwsInvalidState() {
        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                "PR-100",
                1L,
                BigDecimal.ZERO,
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                null,
                null,
                "HIGH",
                null
        );

        assertThatThrownBy(() -> this.productionOrderService.createProductionOrder(command))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    void createProductionOrder_invalidDates_throwsInvalidState() {
        Product product = new Product("PROD-001", "Motor");
        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.productionOrderRepository.existsByProductionNumber("PR-100")).thenReturn(false);

        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                "PR-100",
                1L,
                new BigDecimal("50"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5),
                null,
                null,
                "HIGH",
                null
        );

        assertThatThrownBy(() -> this.productionOrderService.createProductionOrder(command))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Planned start date cannot be after planned end date");
    }

    @Test
    void lifecycleTransitions_success() {
        Product product = new Product("PROD-001", "Motor");
        ProductionOrder order = new ProductionOrder("PR-100", product, new BigDecimal("50"));
        order.setStatus(ProductionOrderService.STATUS_PLANNED);

        when(this.productionOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(this.productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductionOrder inProgress = this.productionOrderService.updateStatus(1L, ProductionOrderService.STATUS_IN_PROGRESS);
        assertThat(inProgress.getStatus()).isEqualTo(ProductionOrderService.STATUS_IN_PROGRESS);
        assertThat(inProgress.getActualStartDate()).isNotNull();

        ProductionOrder completed = this.productionOrderService.updateStatus(1L, ProductionOrderService.STATUS_COMPLETED);
        assertThat(completed.getStatus()).isEqualTo(ProductionOrderService.STATUS_COMPLETED);
        assertThat(completed.getActualEndDate()).isNotNull();
    }

    @Test
    void lifecycleTransitions_invalid_throwsInvalidState() {
        Product product = new Product("PROD-001", "Motor");
        ProductionOrder order = new ProductionOrder("PR-100", product, new BigDecimal("50"));
        order.setStatus(ProductionOrderService.STATUS_PLANNED);

        when(this.productionOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> this.productionOrderService.updateStatus(1L, ProductionOrderService.STATUS_COMPLETED))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Invalid production order transition from PLANNED to COMPLETED");
    }

    @Test
    void cancelProductionOrder_success() {
        Product product = new Product("PROD-001", "Motor");
        ProductionOrder order = new ProductionOrder("PR-100", product, new BigDecimal("50"));
        order.setStatus(ProductionOrderService.STATUS_PLANNED);

        when(this.productionOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(this.productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductionOrder cancelled = this.productionOrderService.cancelProductionOrder(1L);
        assertThat(cancelled.getStatus()).isEqualTo(ProductionOrderService.STATUS_CANCELLED);
    }
}
