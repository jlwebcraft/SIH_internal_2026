package com.sih.supplychain.controller;

import com.sih.supplychain.domain.CustomerOrder;
import com.sih.supplychain.dto.customerorder.CustomerOrderCreateRequest;
import com.sih.supplychain.dto.customerorder.CustomerOrderResponse;
import com.sih.supplychain.dto.customerorder.CustomerOrderStatusUpdateRequest;
import com.sih.supplychain.dto.customerorder.CustomerOrderUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.CustomerOrderService;
import com.sih.supplychain.service.CustomerOrderService.CreateCustomerOrderCommand;
import com.sih.supplychain.service.CustomerOrderService.CustomerOrderItemCommand;
import com.sih.supplychain.service.CustomerOrderService.UpdateCustomerOrderCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customer-orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @PostMapping
    public ResponseEntity<CustomerOrderResponse> createCustomerOrder(
            @Valid @RequestBody CustomerOrderCreateRequest request
    ) {
        List<CustomerOrderItemCommand> itemCommands = request.items().stream()
                .map(item -> new CustomerOrderItemCommand(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        CreateCustomerOrderCommand command = new CreateCustomerOrderCommand(
                request.orderNumber(),
                request.customerName(),
                request.orderDate(),
                request.requiredDeliveryDate(),
                request.priority(),
                itemCommands
        );

        CustomerOrder created = this.customerOrderService.createCustomerOrder(command);
        return ResponseEntity
                .created(URI.create("/api/customer-orders/" + created.getId()))
                .body(OperationalMapper.toCustomerOrderResponse(created));
    }

    @GetMapping("/{id}")
    public CustomerOrderResponse getCustomerOrderById(@PathVariable Long id) {
        return OperationalMapper.toCustomerOrderResponse(this.customerOrderService.getCustomerOrderById(id));
    }

    @GetMapping("/by-number/{orderNumber}")
    public CustomerOrderResponse getCustomerOrderByNumber(@PathVariable String orderNumber) {
        return OperationalMapper.toCustomerOrderResponse(this.customerOrderService.getCustomerOrderByNumber(orderNumber));
    }

    @GetMapping
    public List<CustomerOrderResponse> getCustomerOrders(@RequestParam(required = false) String status) {
        List<CustomerOrder> orders;
        if (status != null && !status.isBlank()) {
            orders = this.customerOrderService.getCustomerOrdersByStatus(status);
        } else {
            orders = this.customerOrderService.getAllCustomerOrders();
        }
        return orders.stream()
                .map(OperationalMapper::toCustomerOrderResponse)
                .toList();
    }

    @PutMapping("/{id}")
    public CustomerOrderResponse updateCustomerOrder(
            @PathVariable Long id,
            @Valid @RequestBody CustomerOrderUpdateRequest request
    ) {
        UpdateCustomerOrderCommand command = new UpdateCustomerOrderCommand(
                request.customerName(),
                request.orderDate(),
                request.requiredDeliveryDate(),
                request.priority()
        );
        CustomerOrder updated = this.customerOrderService.updateCustomerOrder(id, command);
        return OperationalMapper.toCustomerOrderResponse(updated);
    }

    @PutMapping("/{id}/status")
    public CustomerOrderResponse updateCustomerOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerOrderStatusUpdateRequest request
    ) {
        CustomerOrder updated = this.customerOrderService.updateStatus(id, request.status());
        return OperationalMapper.toCustomerOrderResponse(updated);
    }

    @PatchMapping("/{id}/status")
    public CustomerOrderResponse patchCustomerOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerOrderStatusUpdateRequest request
    ) {
        CustomerOrder updated = this.customerOrderService.updateStatus(id, request.status());
        return OperationalMapper.toCustomerOrderResponse(updated);
    }

    @PostMapping("/{id}/cancel")
    public CustomerOrderResponse cancelCustomerOrder(@PathVariable Long id) {
        CustomerOrder cancelled = this.customerOrderService.cancelCustomerOrder(id);
        return OperationalMapper.toCustomerOrderResponse(cancelled);
    }

    @PatchMapping("/{id}/cancel")
    public CustomerOrderResponse patchCancelCustomerOrder(@PathVariable Long id) {
        CustomerOrder cancelled = this.customerOrderService.cancelCustomerOrder(id);
        return OperationalMapper.toCustomerOrderResponse(cancelled);
    }
}
