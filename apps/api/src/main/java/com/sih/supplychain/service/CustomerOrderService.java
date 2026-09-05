package com.sih.supplychain.service;

import com.sih.supplychain.domain.CustomerOrder;
import com.sih.supplychain.domain.CustomerOrderItem;
import com.sih.supplychain.domain.Product;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.CustomerOrderItemRepository;
import com.sih.supplychain.repository.CustomerOrderRepository;
import com.sih.supplychain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CustomerOrderService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_FULFILLED = "FULFILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerOrderItemRepository customerOrderItemRepository;
    private final ProductRepository productRepository;

    public CustomerOrderService(
            CustomerOrderRepository customerOrderRepository,
            CustomerOrderItemRepository customerOrderItemRepository,
            ProductRepository productRepository
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.customerOrderItemRepository = customerOrderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CustomerOrder createCustomerOrder(CreateCustomerOrderCommand command) {
        validateCreateCommand(command);

        if (this.customerOrderRepository.existsByOrderNumber(command.orderNumber())) {
            throw new DuplicateResourceException("Customer order number already exists: " + command.orderNumber());
        }

        LocalDate orderDate = command.orderDate() == null ? LocalDate.now() : command.orderDate();
        validateDateRange(orderDate, command.requiredDeliveryDate(), "Order date cannot be after required delivery date");

        List<ResolvedCustomerOrderItem> resolvedItems = command.items().stream()
                .map(this::resolveItem)
                .toList();

        CustomerOrder order = new CustomerOrder(command.orderNumber(), command.customerName());
        order.setOrderDate(orderDate);
        order.setRequiredDeliveryDate(command.requiredDeliveryDate());
        order.setPriority(command.priority());
        order.setStatus(STATUS_PENDING);
        order.setTotalAmount(calculateTotal(resolvedItems));

        CustomerOrder savedOrder = this.customerOrderRepository.save(order);
        List<CustomerOrderItem> itemsToSave = resolvedItems.stream()
                .map(item -> toCustomerOrderItem(savedOrder, item))
                .toList();
        this.customerOrderItemRepository.saveAll(itemsToSave);
        savedOrder.getItems().addAll(itemsToSave);

        return savedOrder;
    }

    public CustomerOrder getCustomerOrderById(Long id) {
        return this.customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with id: " + id));
    }

    public CustomerOrder getCustomerOrderByNumber(String orderNumber) {
        BusinessValidation.requireText(orderNumber, "Order number");
        return this.customerOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with number: " + orderNumber));
    }

    public List<CustomerOrder> getAllCustomerOrders() {
        return this.customerOrderRepository.findAll();
    }

    public List<CustomerOrder> getCustomerOrdersByStatus(String status) {
        BusinessValidation.requireText(status, "Status");
        return this.customerOrderRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional
    public CustomerOrder updateCustomerOrder(Long id, UpdateCustomerOrderCommand command) {
        CustomerOrder order = getCustomerOrderById(id);
        if (STATUS_FULFILLED.equalsIgnoreCase(order.getStatus()) || STATUS_CANCELLED.equalsIgnoreCase(order.getStatus())) {
            throw new InvalidBusinessStateException("Cannot update customer order in " + order.getStatus() + " status");
        }

        validateUpdateCommand(command);
        LocalDate orderDate = command.orderDate() == null ? order.getOrderDate() : command.orderDate();
        validateDateRange(orderDate, command.requiredDeliveryDate(), "Order date cannot be after required delivery date");

        order.setCustomerName(command.customerName());
        order.setOrderDate(orderDate);
        order.setRequiredDeliveryDate(command.requiredDeliveryDate());
        order.setPriority(command.priority());

        return this.customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder updateStatus(Long id, String targetStatus) {
        BusinessValidation.requireText(targetStatus, "Target status");
        CustomerOrder order = getCustomerOrderById(id);
        String currentStatus = order.getStatus() == null ? STATUS_PENDING : order.getStatus().toUpperCase();
        String nextStatus = targetStatus.trim().toUpperCase();

        if (currentStatus.equals(nextStatus)) {
            return order;
        }

        validateTransition(currentStatus, nextStatus);
        order.setStatus(nextStatus);

        return this.customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder cancelCustomerOrder(Long id) {
        return updateStatus(id, STATUS_CANCELLED);
    }

    private void validateCreateCommand(CreateCustomerOrderCommand command) {
        if (command == null) {
            throw new InvalidBusinessStateException("Customer order create request is required");
        }
        BusinessValidation.requireText(command.orderNumber(), "Order number");
        BusinessValidation.requireText(command.customerName(), "Customer name");
        if (command.items() == null || command.items().isEmpty()) {
            throw new InvalidBusinessStateException("Customer order must contain at least one item");
        }
    }

    private void validateUpdateCommand(UpdateCustomerOrderCommand command) {
        if (command == null) {
            throw new InvalidBusinessStateException("Customer order update request is required");
        }
        BusinessValidation.requireText(command.customerName(), "Customer name");
    }

    private void validateDateRange(LocalDate start, LocalDate end, String errorMessage) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidBusinessStateException(errorMessage);
        }
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        boolean valid = switch (currentStatus) {
            case STATUS_PENDING -> Set.of(STATUS_CONFIRMED, STATUS_CANCELLED).contains(targetStatus);
            case STATUS_CONFIRMED -> Set.of(STATUS_IN_PROGRESS, STATUS_CANCELLED).contains(targetStatus);
            case STATUS_IN_PROGRESS -> Set.of(STATUS_FULFILLED, STATUS_CANCELLED).contains(targetStatus);
            default -> false;
        };

        if (!valid) {
            throw new InvalidBusinessStateException(
                    "Invalid customer order transition from " + currentStatus + " to " + targetStatus
            );
        }
    }

    private ResolvedCustomerOrderItem resolveItem(CustomerOrderItemCommand itemCommand) {
        if (itemCommand == null) {
            throw new InvalidBusinessStateException("Customer order item cannot be null");
        }
        if (itemCommand.productId() == null) {
            throw new InvalidBusinessStateException("Product ID is required for each order item");
        }
        BusinessValidation.requirePositive(itemCommand.quantity(), "Item quantity");
        BusinessValidation.requireNonNegative(itemCommand.unitPrice(), "Item unit price");

        Product product = this.productRepository.findById(itemCommand.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemCommand.productId()));

        BigDecimal unitPrice = itemCommand.unitPrice() == null ? BigDecimal.ZERO : itemCommand.unitPrice();
        return new ResolvedCustomerOrderItem(product, itemCommand.quantity(), unitPrice);
    }

    private BigDecimal calculateTotal(List<ResolvedCustomerOrderItem> items) {
        return items.stream()
                .map(item -> item.quantity().multiply(item.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private CustomerOrderItem toCustomerOrderItem(CustomerOrder order, ResolvedCustomerOrderItem resolved) {
        CustomerOrderItem item = new CustomerOrderItem(order, resolved.product(), resolved.quantity());
        item.setUnitPrice(resolved.unitPrice());
        return item;
    }

    public record CreateCustomerOrderCommand(
            String orderNumber,
            String customerName,
            LocalDate orderDate,
            LocalDate requiredDeliveryDate,
            String priority,
            List<CustomerOrderItemCommand> items
    ) {
    }

    public record CustomerOrderItemCommand(
            Long productId,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {
    }

    public record UpdateCustomerOrderCommand(
            String customerName,
            LocalDate orderDate,
            LocalDate requiredDeliveryDate,
            String priority
    ) {
    }

    private record ResolvedCustomerOrderItem(
            Product product,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {
    }
}
