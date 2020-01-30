package com.example.demo.test.mock;

import java.net.URI;
import java.net.URL;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.util.JsonPathExpectationsHelper;

public class MockMvc {
	private Client client;
	private URI baseUri;
	private PORT_HANDLING portHandling;

	public void setPORTHANDLING(PORT_HANDLING portHandling) {
		this.portHandling = portHandling;
	}

	public void setBaseUri(URI baseUri) {
		this.baseUri = baseUri;
	}

	private Response response;
	private String json;

	public void setClient(Client client) {
		this.client = client;
	}

	public MockMvc perform(Pair<String, String> methodPath) {
		String httpMethod = methodPath.getFirst();
		String path = methodPath.getSecond();
		if (HttpMethod.GET.equals(httpMethod)) {
			Response response = client
					.target(String.format("%s://%s%s", baseUri.getScheme(), baseUri.getAuthority(), path)).request()
					.get();
			setResponse(response);
		} else if (HttpMethod.POST.equals(httpMethod)) {
			Response response = client
					.target(String.format("%s://%s%s", baseUri.getScheme(), baseUri.getAuthority(), path)).request()
					.post(Entity.json(null));
			setResponse(response);
		}
		return this;
	}

	public String getJson() {
		return json;
	}

	public void setResponse(Response response) {
		this.response = response;
		this.json = (String) response.readEntity(String.class);
	}

	public MockMvc andDo(COMMAND command) {
		return this;
	}

	public static COMMAND print() {
		return COMMAND.PRINT;
	}

	public static Pair<String, String> get(String path) {
		return Pair.of(HttpMethod.GET, path);
	}

	public static Pair<String, String> post(String path) {
		return Pair.of(HttpMethod.POST, path);
	}

	public static Status status() {
		Status status = new Status();
		return status;
	}

	public static Content content() {
		Content content = new Content();
		return content;
	}

	public static COMMAND jsonPath(String expression, String expected) {
		COMMAND.JSON_PATH.setExpression(expression);
		COMMAND.JSON_PATH.setExpected(expected);
		return COMMAND.JSON_PATH;
	}

	public static String is(String expected) {
		return expected;
	}

	public static String is(long expected) {
		return Long.toString(expected);
	}

	public MockMvc andExpect(MockMvc.COMMAND command) throws Exception {
		switch (command) {
		case PRINT:
			break;
		case STATUS_OK: {
			if (!Response.Status.OK.equals(Response.Status.fromStatusCode(response.getStatus())))
				throw new Exception();
			break;
		}
		case CONTENT_HAL_JSON: {
			String contentType = response.getHeaderString("Content-Type");
			if (!(MediaTypes.HAL_JSON).equals(MediaType.valueOf(contentType)))
				throw new Exception();
			break;
		}
		case JSON_PATH: {
			String expression = COMMAND.JSON_PATH.getExpression();
			String expected = COMMAND.JSON_PATH.expected;
			String json = getJson();
			Object[] args = {};
			JsonPathExpectationsHelper helper = new JsonPathExpectationsHelper(expression, args);

			Object rawActual = helper.evaluateJsonPath(json);
			boolean isNumeric = expected.chars().allMatch(Character::isDigit);
			if (isNumeric) {
				Integer actualValue = (Integer) rawActual;
				rawActual = String.format("%d", actualValue);
			}
			String actualUri = (String) rawActual;
			String expectedUri = expected;
			if (PORT_HANDLING.SET_EXPECTED_PORT_FROM_ACTUAL.equals(portHandling) && expectedUri.startsWith("http://")) {
				// very naive approach, but it is good enough for demo
				URL originalURL = new URL(expected);
				// add port
				URL portURL = new URL(originalURL.getProtocol(), originalURL.getHost(), baseUri.getPort(),
						originalURL.getFile());
				expectedUri = portURL.toString();
			}
			if (!expectedUri.equals(actualUri))
				throw new Exception();

			break;
		}
		case IS4XXCLIENTERROR: {
			if (!Response.Status.Family.CLIENT_ERROR.equals(Response.Status.Family.familyOf(response.getStatus())))
				throw new Exception();
			break;
		}
		case CONTENT_APPLICATION_JSON: {
			String contentType = response.getHeaderString("Content-Type");
			if (!(MediaType.APPLICATION_JSON).equals(MediaType.valueOf(contentType)))
				throw new Exception();
			break;
		}
		case CONTENT_STRING: {
			String expected = COMMAND.CONTENT_STRING.getExpected();
			String json = getJson();
			if (!json.equals(expected))
				throw new Exception();
			break;
		}
		default:
			throw new Exception();
		}

		return this;
	}
	
	public static enum PORT_HANDLING{
		SET_EXPECTED_PORT_FROM_ACTUAL, SET_ACTUAL_PORT_FROM_EXPECTED, DO_NOTHING
	}

	public static enum COMMAND {
		PRINT, STATUS_OK, CONTENT_HAL_JSON, CONTENT_UNKNOWN, IS4XXCLIENTERROR, CONTENT_APPLICATION_JSON, JSON_PATH {
			@Override
			public void setExpression(String expression) {
				this.expression = expression;
			}

			@Override
			public void setExpected(String expected) {
				this.expected = expected;
			}
		},
		CONTENT_STRING {
			@Override
			public void setExpected(String expected) {
				this.expected = expected;
			}
		};

		protected String expression;
		protected String expected;

		public void setExpression(String expression) {
		}

		public void setExpected(String expected) {
		}

		public String getExpression() {
			return expression;
		}

		public String getExpected() {
			return expected;
		}
	}

	public static class Status {
		public COMMAND isOk() {
			return COMMAND.STATUS_OK;
		}

		public COMMAND is4xxClientError() {
			return COMMAND.IS4XXCLIENTERROR;
		}
	}

	public static class Content {
		public COMMAND contentType(MediaType mediaType) {
			if (MediaTypes.HAL_JSON.equals(mediaType)) {
				return COMMAND.CONTENT_HAL_JSON;
			} else if (MediaType.APPLICATION_JSON.equals(mediaType)) {
				return COMMAND.CONTENT_APPLICATION_JSON;
			}
			return COMMAND.CONTENT_UNKNOWN;
		}

		public COMMAND string(String expected) {
			COMMAND.CONTENT_STRING.setExpected(expected);
			return COMMAND.CONTENT_STRING;
		}
	}

	public static class JsonPath {

	}

}
