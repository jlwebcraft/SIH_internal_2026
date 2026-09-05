package com.sih.supplychain.service;

import com.sih.supplychain.domain.CustomerOrder;
import com.sih.supplychain.domain.Product;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.CustomerOrderItemRepository;
import com.sih.supplychain.repository.CustomerOrderRepository;
import com.sih.supplychain.repository.ProductRepository;
import com.sih.supplychain.service.CustomerOrderService.CreateCustomerOrderCommand;
import com.sih.supplychain.service.CustomerOrderService.CustomerOrderItemCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTests {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private CustomerOrderItemRepository customerOrderItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CustomerOrderService customerOrderService;

    @BeforeEach
    void setUp() {
        this.customerOrderService = new CustomerOrderService(
                this.customerOrderRepository,
                this.customerOrderItemRepository,
                this.productRepository
        );
    }

    @Test
    void createCustomerOrder_success_calculatesTotalAmount() {
        Product prodA = new Product("PROD-001", "Motor");
        Product prodB = new Product("PROD-002", "Pump");

        when(this.customerOrderRepository.existsByOrderNumber("CO-1001")).thenReturn(false);
        when(this.productRepository.findById(1L)).thenReturn(Optional.of(prodA));
        when(this.productRepository.findById(2L)).thenReturn(Optional.of(prodB));
        when(this.customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                "CO-1001",
                "Acme Corp",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "HIGH",
                List.of(
                        new CustomerOrderItemCommand(1L, new BigDecimal("10"), new BigDecimal("150.00")),
                        new CustomerOrderItemCommand(2L, new BigDecimal("5"), new BigDecimal("200.50"))
                )
        );

        CustomerOrder created = this.customerOrderService.createCustomerOrder(command);

        assertThat(created).isNotNull();
        assertThat(created.getOrderNumber()).isEqualTo("CO-1001");
        assertThat(created.getCustomerName()).isEqualTo("Acme Corp");
        assertThat(created.getStatus()).isEqualTo(CustomerOrderService.STATUS_PENDING);
        // (10 * 150.00) + (5 * 200.50) = 1500.00 + 1002.50 = 2502.50
        assertThat(created.getTotalAmount()).isEqualByComparingTo("2502.50");
        assertThat(created.getItems()).hasSize(2);
    }

    @Test
    void createCustomerOrder_duplicateOrderNumber_throwsDuplicate() {
        when(this.customerOrderRepository.existsByOrderNumber("CO-1001")).thenReturn(true);

        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                "CO-1001",
                "Acme Corp",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "HIGH",
                List.of(new CustomerOrderItemCommand(1L, new BigDecimal("10"), new BigDecimal("150.00")))
        );

        assertThatThrownBy(() -> this.customerOrderService.createCustomerOrder(command))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Customer order number already exists");

        verify(this.customerOrderRepository, never()).save(any());
    }

    @Test
    void createCustomerOrder_missingProduct_throwsNotFound() {
        when(this.customerOrderRepository.existsByOrderNumber("CO-1001")).thenReturn(false);
        when(this.productRepository.findById(99L)).thenReturn(Optional.empty());

        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                "CO-1001",
                "Acme Corp",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "HIGH",
                List.of(new CustomerOrderItemCommand(99L, new BigDecimal("10"), new BigDecimal("150.00")))
        );

        assertThatThrownBy(() -> this.customerOrderService.createCustomerOrder(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");

        verify(this.customerOrderRepository, never()).save(any());
    }

    @Test
    void createCustomerOrder_emptyItems_throwsInvalidState() {
        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                "CO-1001",
                "Acme Corp",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "HIGH",
                Collections.emptyList()
        );

        assertThatThrownBy(() -> this.customerOrderService.createCustomerOrder(command))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Customer order must contain at least one item");
    }

    @Test
    void createCustomerOrder_invalidQuantity_throwsInvalidState() {
        when(this.customerOrderRepository.existsByOrderNumber("CO-1001")).thenReturn(false);

        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                "CO-1001",
                "Acme Corp",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "HIGH",
                List.of(new CustomerOrderItemCommand(1L, BigDecimal.ZERO, new BigDecimal("150.00")))
        );

        assertThatThrownBy(() -> this.customerOrderService.createCustomerOrder(command))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Item quantity must be greater than zero");
    }

    @Test
    void createCustomerOrder_negativeUnitPrice_throwsInvalidState() {
        when(this.customerOrderRepository.existsByOrderNumber("CO-1001")).thenReturn(false);

        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                "CO-1001",
                "Acme Corp",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "HIGH",
                List.of(new CustomerOrderItemCommand(1L, new BigDecimal("10"), new BigDecimal("-50.00")))
        );

        assertThatThrownBy(() -> this.customerOrderService.createCustomerOrder(command))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Item unit price cannot be negative");
    }

    @Test
    void lifecycleTransitions_success() {
        CustomerOrder order = new CustomerOrder("CO-1001", "Acme Corp");
        order.setStatus(CustomerOrderService.STATUS_PENDING);

        when(this.customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(this.customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder confirmed = this.customerOrderService.updateStatus(1L, CustomerOrderService.STATUS_CONFIRMED);
        assertThat(confirmed.getStatus()).isEqualTo(CustomerOrderService.STATUS_CONFIRMED);

        CustomerOrder inProgress = this.customerOrderService.updateStatus(1L, CustomerOrderService.STATUS_IN_PROGRESS);
        assertThat(inProgress.getStatus()).isEqualTo(CustomerOrderService.STATUS_IN_PROGRESS);

        CustomerOrder fulfilled = this.customerOrderService.updateStatus(1L, CustomerOrderService.STATUS_FULFILLED);
        assertThat(fulfilled.getStatus()).isEqualTo(CustomerOrderService.STATUS_FULFILLED);
    }

    @Test
    void lifecycleTransitions_invalid_throwsInvalidState() {
        CustomerOrder order = new CustomerOrder("CO-1001", "Acme Corp");
        order.setStatus(CustomerOrderService.STATUS_PENDING);

        when(this.customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> this.customerOrderService.updateStatus(1L, CustomerOrderService.STATUS_FULFILLED))
                .isInstanceOf(InvalidBusinessStateException.class)
                .hasMessageContaining("Invalid customer order transition from PENDING to FULFILLED");
    }

    @Test
    void cancelCustomerOrder_success() {
        CustomerOrder order = new CustomerOrder("CO-1001", "Acme Corp");
        order.setStatus(CustomerOrderService.STATUS_PENDING);

        when(this.customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(this.customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder cancelled = this.customerOrderService.cancelCustomerOrder(1L);
        assertThat(cancelled.getStatus()).isEqualTo(CustomerOrderService.STATUS_CANCELLED);
    }
}
