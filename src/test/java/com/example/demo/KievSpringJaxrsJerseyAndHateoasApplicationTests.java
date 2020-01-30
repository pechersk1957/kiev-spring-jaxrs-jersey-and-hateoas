package com.example.demo;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.config.HateoasConfiguration;
import org.springframework.http.MediaType;

import com.example.demo.configuration.MockMvcConfig;
import com.example.demo.controller.CustomOrderHateoasController;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import com.example.demo.test.mock.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.example.demo.test.mock.MockMvc.get;
import static com.example.demo.test.mock.MockMvc.print;
import static com.example.demo.test.mock.MockMvc.status;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static com.example.demo.test.mock.MockMvc.content;
import static com.example.demo.test.mock.MockMvc.is;
import static com.example.demo.test.mock.MockMvc.jsonPath;
import static com.example.demo.test.mock.MockMvc.post;

@SpringBootTest()
public class KievSpringJaxrsJerseyAndHateoasApplicationTests extends JerseyTest {

	private MockMvc mvc;

	@Override
	protected Application configure() {

		final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				KievSpringJaxrsJerseyAndHateoasApplication.class, HateoasConfiguration.class, MockMvcConfig.class);

		mvc = context.containsBean("mockMvc") ? context.getBean(MockMvc.class) : new MockMvc();

		// load test data
		if (context.containsBean("orderRepository")) {
			OrderRepository repository = context.getBean(OrderRepository.class);
			repository.save(new Order("grande mocha"));
			repository.save(new Order("venti hazelnut machiatto"));
		}

		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.property("contextConfig", context);
		resourceConfig.register(CustomOrderHateoasController.class);
		Optional<ObjectMapper> mapperSpring = Optional
				.ofNullable(KievSpringJaxrsJerseyAndHateoasApplication.mapperSpring);
		if (mapperSpring.isPresent()) {
			JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
			provider.setMapper(mapperSpring.get());
			resourceConfig.register(provider);
		}
		return resourceConfig;
	}

	@Before
	public void Setup() {
		this.mvc.setClient(this.client());
		URI baseUri = this.getBaseUri();
		this.mvc.setBaseUri(baseUri);
	}

	@Test
	public void basics() throws Exception {

		this.mvc.perform(get("/api")).andDo(print()).andExpect(status().isOk())
				.andExpect(content().contentType(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links.orders.href", is("http://localhost/api/orders")))
				.andExpect(jsonPath("$._links.profile.href", is("http://localhost/api/alps")));

		this.mvc.perform(get("/api/orders")).andDo(print()) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType(MediaTypes.HAL_JSON)) //
				.andExpect(jsonPath("$._embedded.orders[0].orderStatus", is("BEING_CREATED")))
				.andExpect(jsonPath("$._embedded.orders[0].description", is("grande mocha")))
				.andExpect(jsonPath("$._embedded.orders[0]._links.self.href", is("http://localhost/api/orders/1")))
				.andExpect(jsonPath("$._embedded.orders[0]._links.order.href", is("http://localhost/api/orders/1")))
				.andExpect(
						jsonPath("$._embedded.orders[0]._links.payment.href", is("http://localhost/api/orders/1/pay")))
				.andExpect(jsonPath("$._embedded.orders[0]._links.cancel.href",
						is("http://localhost/api/orders/1/cancel")))
				.andExpect(jsonPath("$._embedded.orders[1].orderStatus", is("BEING_CREATED")))
				.andExpect(jsonPath("$._embedded.orders[1].description", is("venti hazelnut machiatto")))
				.andExpect(jsonPath("$._embedded.orders[1]._links.self.href", is("http://localhost/api/orders/2")))
				.andExpect(jsonPath("$._embedded.orders[1]._links.order.href", is("http://localhost/api/orders/2")))
				.andExpect(
						jsonPath("$._embedded.orders[1]._links.payment.href", is("http://localhost/api/orders/2/pay")))
				.andExpect(jsonPath("$._embedded.orders[1]._links.cancel.href",
						is("http://localhost/api/orders/2/cancel")))
				.andExpect(jsonPath("$._links.self.href", is("http://localhost/api/orders")))
				.andExpect(jsonPath("$._links.profile.href", is("http://localhost/api/profile/orders")));

		// Fulfilling an unpaid-for order should fail.

		this.mvc.perform(post("/api/orders/1/fulfill")) //
				.andDo(print()).andExpect(status().is4xxClientError()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(content().string("\"Transitioning from BEING_CREATED to FULFILLED is not valid.\""));

		// Pay for the order.

		this.mvc.perform(post("/api/orders/1/pay")) //
				.andDo(print()) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(jsonPath("$.id", is(1))) //
				.andExpect(jsonPath("$.orderStatus", is("PAID_FOR")));

		// Paying for an already paid-for order should fail.

		this.mvc.perform(post("/api/orders/1/pay")) //
				.andDo(print()) //
				.andExpect(status().is4xxClientError()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(content().string("\"Transitioning from PAID_FOR to PAID_FOR is not valid.\""));

		// Cancelling a paid-for order should fail.

		this.mvc.perform(post("/api/orders/1/cancel")) //
				.andDo(print()) //
				.andExpect(status().is4xxClientError()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(content().string("\"Transitioning from PAID_FOR to CANCELLED is not valid.\""));

		// Verify a paid-for order now shows links to fulfill.

		this.mvc.perform(get("/api/orders/1")) //
				.andDo(print()) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType(MediaTypes.HAL_JSON)) //
				.andExpect(jsonPath("$.orderStatus", is("PAID_FOR"))) //
				.andExpect(jsonPath("$.description", is("grande mocha"))) //
				.andExpect(jsonPath("$._links.self.href", is("http://localhost/api/orders/1")))
				.andExpect(jsonPath("$._links.order.href", is("http://localhost/api/orders/1")))
				.andExpect(jsonPath("$._links.fulfill.href", is("http://localhost/api/orders/1/fulfill")));

		// Fulfill the order.

		this.mvc.perform(post("/api/orders/1/fulfill")) //
				.andDo(print()) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(jsonPath("$.orderStatus", is("FULFILLED"))) //
				.andExpect(jsonPath("$.description", is("grande mocha")));

		// Cancelling a fulfilled order should fail.

		this.mvc.perform(post("/api/orders/1/cancel")) //
				.andDo(print()) //
				.andExpect(status().is4xxClientError()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(content().string("\"Transitioning from FULFILLED to CANCELLED is not valid.\""));

		// Cancel an order.

		this.mvc.perform(post("/api/orders/2/cancel")) //
				.andDo(print()) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
				.andExpect(jsonPath("$.orderStatus", is("CANCELLED"))) //
				.andExpect(jsonPath("$.description", is("venti hazelnut machiatto")));
	}

}
