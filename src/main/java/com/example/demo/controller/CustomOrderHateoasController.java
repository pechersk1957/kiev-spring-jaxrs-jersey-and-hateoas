package com.example.demo.controller;

import static com.example.demo.controller.OrderStatus.valid;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import com.example.demo.controller.OrdersModel.RootResourceDiscoverability;
import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;

@Component
@Path("/api")
public class CustomOrderHateoasController {
	private final OrderRepository repository;
	
	public CustomOrderHateoasController(OrderRepository repository) {
		this.repository = repository;
	}
	
	@GET
	@Consumes("application/json, application/hal+json")
	@Produces("application/hal+json")
	public RootResourceDiscoverability rootResouceDiscoverability(@Context UriInfo uriInfo) {
		String href = uriInfo.getAbsolutePath().toString();
		return new RootResourceDiscoverability(href);
	}

	@GET
	@Path("orders")
	@Consumes("application/hal+json")
	@Produces("application/hal+json")
	public OrdersModel findOrders(@Context UriInfo uriInfo) {
		Iterable<Order> iterator = repository.findAll();
		OrdersModel model = new OrdersModel(iterator, uriInfo);
		String href = uriInfo.getAbsolutePath().toString();
		String hrefProfile = href.replace("/orders", "/profile/orders");
		Link selfLink = new Link(href).withSelfRel();
		Link profileLink = new Link(hrefProfile).withRel("profile");
		model.add(selfLink, profileLink);

		return model;
	}

	@GET
	@Path("orders/{id}/")
	@Consumes("application/json, application/hal+json")
	@Produces("application/hal+json")
	public EntityModel<Order> findOrder(@PathParam("id") Long id, @Context UriInfo uriInfo) {
		Order order = this.repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
		String href = uriInfo.getAbsolutePath().toString();
		EntityModel<Order> model = new EntityModel<Order>(order);
		OrdersModel.addOrderLinkOperation(model, href);
		return model;
	}

	@POST
	@Path("orders/{id}/pay")
	@Consumes("application/json, application/hal+json")
	@Produces("application/json")
	public Response pay(@PathParam("id") Long id) {
		Order order = this.repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

		if (valid(order.getOrderStatus(), OrderStatus.PAID_FOR)) {

			order.setOrderStatus(OrderStatus.PAID_FOR);
			return Response.ok(repository.save(order)).build();
		}

		return Response.status(Response.Status.BAD_REQUEST).entity(
				"\"Transitioning from " + order.getOrderStatus() + " to " + OrderStatus.PAID_FOR + " is not valid.\"")
				.build();

	}

	@POST
	@Path("orders/{id}/cancel")
	@Consumes("application/json, application/hal+json")
	@Produces("application/json")
	public Response cancel(@PathParam("id") Long id) {
		Order order = this.repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

		if (valid(order.getOrderStatus(), OrderStatus.CANCELLED)) {

			order.setOrderStatus(OrderStatus.CANCELLED);
			return Response.ok(repository.save(order)).build();
		}

		return Response.status(Response.Status.BAD_REQUEST).entity(
				"\"Transitioning from " + order.getOrderStatus() + " to " + OrderStatus.CANCELLED + " is not valid.\"")
				.build();
	}

	@POST
	@Path("orders/{id}/fulfill")
	@Consumes("application/json, application/hal+json")
	@Produces("application/json")
	public Response fulfill(@PathParam("id") Long id) {
		Order order = this.repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

		if (valid(order.getOrderStatus(), OrderStatus.FULFILLED)) {

			order.setOrderStatus(OrderStatus.FULFILLED);
			return Response.ok(repository.save(order)).build();
		}

		return Response.status(Response.Status.BAD_REQUEST).entity(
				"\"Transitioning from " + order.getOrderStatus() + " to " + OrderStatus.FULFILLED + " is not valid.\"")
				.build();
	}

}
