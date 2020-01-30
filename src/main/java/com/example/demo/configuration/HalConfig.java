package com.example.demo.configuration;

import java.util.Collections;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;

import com.example.demo.KievSpringJaxrsJerseyAndHateoasApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class HalConfig {
	@Bean
	ObjectMapper getMapper(Optional<LinkRelationProvider> linkProvider, Optional<CurieProvider> providerCurie,
			Optional<HalConfiguration> configurationHal, MessageResolver resolver) {
		ObjectMapper mapper = new ObjectMapper();
		LinkRelationProvider defaultedRelProvider = linkProvider.orElseGet(EvoInflectorLinkRelationProvider::new);
		CurieProvider curieProvider = providerCurie.orElse(new DefaultCurieProvider(Collections.emptyMap(), null));
		HalConfiguration halConfiguration = configurationHal.orElseGet(HalConfiguration::new);
		mapper.registerModule(new Jackson2HalModule());
		mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(defaultedRelProvider, curieProvider,
				resolver, halConfiguration));
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		KievSpringJaxrsJerseyAndHateoasApplication.mapperSpring = mapper;
		return mapper;
	}

}
