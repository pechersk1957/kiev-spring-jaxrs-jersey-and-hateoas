package com.example.demo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.repository.OrderRepository;
import com.example.demo.test.mock.MockMvc;
import com.example.demo.test.mock.MockMvc.PORT_HANDLING;

@Configuration
public class MockMvcConfig {
	@Bean(name="mockMvc")
	MockMvc getMockMvc(OrderRepository repository) {
		MockMvc mvc = new MockMvc();
		mvc.setPORTHANDLING(PORT_HANDLING.SET_EXPECTED_PORT_FROM_ACTUAL);
		return mvc;
	}
}
