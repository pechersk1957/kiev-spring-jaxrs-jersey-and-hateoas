package com.example.demo.controller;

import static com.example.demo.controller.OrderStatus.valid;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

import com.example.demo.model.Order;

public class OrdersModel extends RepresentationModel<OrdersModel> {
	Map.Entry<String, List<EntityModel<Order>>> _embedded;

	public static void addOrderLinkOperation(EntityModel<Order> model, String href) {
		Link selfLink = new Link(href).withSelfRel();
		Link orderLink = new Link(href).withRel("order");
		model.add(selfLink, orderLink);

		// If PAID_FOR is valid, add a link to the `pay()` method
		if (valid(model.getContent().getOrderStatus(), OrderStatus.PAID_FOR)) {
			Link paidLink = new Link(String.format("%s/%s", href, "pay"), "payment");
			model.add(paidLink);
		}

		// If CANCELLED is valid, add a link to the `cancel()` method
		if (valid(model.getContent().getOrderStatus(), OrderStatus.CANCELLED)) {
			Link cancelLink = new Link(String.format("%s/%s", href, "cancel"), "cancel");
			model.add(cancelLink);
		}

		// If FULFILLED is valid, add a link to the `fulfill()` method
		if (valid(model.getContent().getOrderStatus(), OrderStatus.FULFILLED)) {
			Link fulfilLink = new Link(String.format("%s/%s", href, "fulfill"), "fulfill");
			model.add(fulfilLink);
		}
	}

	public OrdersModel(Iterable<Order> models, UriInfo uriInfo) {
		List<EntityModel<Order>> orders = new ArrayList<>();
		for (Order modelOrder : models) {
			String href = uriInfo.getAbsolutePath().toString();
			EntityModel<Order> entityModel = new EntityModel<Order>(modelOrder);
			href = String.format("%s/%d", href, entityModel.getContent().getId());
			addOrderLinkOperation(entityModel, href);
			orders.add(entityModel);
		}
		_embedded = new AbstractMap.SimpleEntry<String, List<EntityModel<Order>>>("orders", orders);
	}

	public Map.Entry<String, List<EntityModel<Order>>> get_embedded() {
		return _embedded;
	}

	public static class RootResourceDiscoverability {
		final WrapperLinks _links;

		public RootResourceDiscoverability(String href) {
			String hrefOrders = String.format("%s/orders", href);
			String hrefProfile = String.format("%s/alps", href);
			Link orderLink = new Link(hrefOrders).withRel("orders");
			Link profileLink = new Link(hrefProfile).withRel("profile");
			WrapperLinks linkOrder = new WrapperLinks(orderLink, profileLink);
			_links = linkOrder;
		}

		public WrapperLinks get_links() {
			return _links;
		}

		class WrapperLinks {
			Link orders;
			Link profile;

			WrapperLinks(Link link, Link profile) {
				this.orders = link;
				this.profile = profile;
			}

			public Link getOrders() {
				return orders;
			}

			public Link getProfile() {
				return profile;
			}

		}
	}

}
