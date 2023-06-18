package com.example.orderservice.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.example.orderservice.dto.InventoryResponse;
import org.springframework.stereotype.Service;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
	private final OrderRepository orderRepository;

	private final WebClient.Builder webClientBuilder;
	
	public void placeOrder(OrderRequest orderRequest) {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());
		List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
			.stream()
			.map(this::mapToDto).toList();
		
		order.setOrderLineItemsList(orderLineItems);

		List<String> skuCodes = order.getOrderLineItemsList().stream()
				.map(OrderLineItems::getSkuCode)
				.toList();

		// Call Inventory Service, and place order if product is in stock
		InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
				.uri("http://inventory-service/api/inventory",
						uriBuilder -> uriBuilder.queryParam ("skuCode", skuCodes).build())
				.retrieve()
				.bodyToMono(InventoryResponse[].class)
				.block();

		boolean allProductsInStock = Arrays.stream(inventoryResponsesArray)
				.allMatch(InventoryResponse::isInStock);

		if(allProductsInStock){
			orderRepository.save(order);
		}else{
			throw new IllegalArgumentException("Product is not in stock, please try again later");
		}

	}
	
	private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
		OrderLineItems orderLineItems = new OrderLineItems();
		orderLineItems.setPrice(orderLineItemsDto.getPrice());
		orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
		orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
		return orderLineItems;
	}
}
