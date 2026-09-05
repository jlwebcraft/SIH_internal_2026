package com.sih.supplychain.service;

import com.sih.supplychain.domain.Product;
import com.sih.supplychain.domain.ProductionOrder;
import com.sih.supplychain.domain.User;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.ProductRepository;
import com.sih.supplychain.repository.ProductionOrderRepository;
import com.sih.supplychain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProductionOrderService {

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductionOrderService(
            ProductionOrderRepository productionOrderRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProductionOrder createProductionOrder(CreateProductionOrderCommand command) {
        validateCreateCommand(command);
        Product product = getProduct(command.productId());

        if (this.productionOrderRepository.existsByProductionNumber(command.productionNumber())) {
            throw new DuplicateResourceException("Production order number already exists: " + command.productionNumber());
        }

        validateDateRange(command.plannedStartDate(), command.plannedEndDate(), "Planned start date cannot be after planned end date");
        validateDateRange(command.actualStartDate(), command.actualEndDate(), "Actual start date cannot be after actual end date");

        User creator = resolveUser(command.createdBy());

        ProductionOrder order = new ProductionOrder(command.productionNumber(), product, command.quantity());
        order.setPlannedStartDate(command.plannedStartDate());
        order.setPlannedEndDate(command.plannedEndDate());
        order.setActualStartDate(command.actualStartDate());
        order.setActualEndDate(command.actualEndDate());
        order.setPriority(command.priority());
        order.setCreatedBy(creator);
        order.setStatus(STATUS_PLANNED);

        return this.productionOrderRepository.save(order);
    }

    public ProductionOrder getProductionOrderById(Long id) {
        return this.productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production order not found with id: " + id));
    }

    public ProductionOrder getProductionOrderByNumber(String productionNumber) {
        BusinessValidation.requireText(productionNumber, "Production number");
        return this.productionOrderRepository.findByProductionNumber(productionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Production order not found with number: " + productionNumber));
    }

    public List<ProductionOrder> getAllProductionOrders() {
        return this.productionOrderRepository.findAll();
    }

    public List<ProductionOrder> getProductionOrdersByProduct(Long productId) {
        getProduct(productId);
        return this.productionOrderRepository.findByProductId(productId);
    }

    public List<ProductionOrder> getProductionOrdersByStatus(String status) {
        BusinessValidation.requireText(status, "Status");
        return this.productionOrderRepository.findByStatus(status.trim().toUpperCase());
    }

    @Transactional
    public ProductionOrder updateProductionOrder(Long id, UpdateProductionOrderCommand command) {
        ProductionOrder order = getProductionOrderById(id);
        if (STATUS_COMPLETED.equalsIgnoreCase(order.getStatus()) || STATUS_CANCELLED.equalsIgnoreCase(order.getStatus())) {
            throw new InvalidBusinessStateException("Cannot update production order in " + order.getStatus() + " status");
        }

        validateUpdateCommand(command);
        validateDateRange(command.plannedStartDate(), command.plannedEndDate(), "Planned start date cannot be after planned end date");
        validateDateRange(command.actualStartDate(), command.actualEndDate(), "Actual start date cannot be after actual end date");

        User creator = resolveUser(command.createdBy());

        order.setQuantity(command.quantity());
        order.setPlannedStartDate(command.plannedStartDate());
        order.setPlannedEndDate(command.plannedEndDate());
        order.setActualStartDate(command.actualStartDate());
        order.setActualEndDate(command.actualEndDate());
        order.setPriority(command.priority());
        if (command.createdBy() != null) {
            order.setCreatedBy(creator);
        }

        return this.productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder updateStatus(Long id, String targetStatus) {
        BusinessValidation.requireText(targetStatus, "Target status");
        ProductionOrder order = getProductionOrderById(id);
        String currentStatus = order.getStatus() == null ? STATUS_PLANNED : order.getStatus().toUpperCase();
        String nextStatus = targetStatus.trim().toUpperCase();

        if (currentStatus.equals(nextStatus)) {
            return order;
        }

        validateTransition(currentStatus, nextStatus);
        order.setStatus(nextStatus);

        if (STATUS_IN_PROGRESS.equals(nextStatus) && order.getActualStartDate() == null) {
            order.setActualStartDate(LocalDate.now());
        } else if (STATUS_COMPLETED.equals(nextStatus) && order.getActualEndDate() == null) {
            order.setActualEndDate(LocalDate.now());
        }

        return this.productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder cancelProductionOrder(Long id) {
        return updateStatus(id, STATUS_CANCELLED);
    }

    private void validateCreateCommand(CreateProductionOrderCommand command) {
        if (command == null) {
            throw new InvalidBusinessStateException("Production order create request is required");
        }
        BusinessValidation.requireText(command.productionNumber(), "Production number");
        if (command.productId() == null) {
            throw new InvalidBusinessStateException("Product ID is required");
        }
        BusinessValidation.requirePositive(command.quantity(), "Quantity");
    }

    private void validateUpdateCommand(UpdateProductionOrderCommand command) {
        if (command == null) {
            throw new InvalidBusinessStateException("Production order update request is required");
        }
        BusinessValidation.requirePositive(command.quantity(), "Quantity");
    }

    private void validateDateRange(LocalDate start, LocalDate end, String errorMessage) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidBusinessStateException(errorMessage);
        }
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        boolean valid = switch (currentStatus) {
            case STATUS_PLANNED -> Set.of(STATUS_IN_PROGRESS, STATUS_CANCELLED).contains(targetStatus);
            case STATUS_IN_PROGRESS -> Set.of(STATUS_COMPLETED, STATUS_CANCELLED).contains(targetStatus);
            default -> false;
        };

        if (!valid) {
            throw new InvalidBusinessStateException(
                    "Invalid production order transition from " + currentStatus + " to " + targetStatus
            );
        }
    }

    private Product getProduct(Long productId) {
        return this.productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public record CreateProductionOrderCommand(
            String productionNumber,
            Long productId,
            BigDecimal quantity,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            String priority,
            Long createdBy
    ) {
    }

    public record UpdateProductionOrderCommand(
            BigDecimal quantity,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            String priority,
            Long createdBy
    ) {
    }
}
