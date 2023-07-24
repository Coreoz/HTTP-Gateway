package com.coreoz.play.responses;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import play.mvc.Http.MimeTypes;
import play.twirl.api.Content;

public class JsonContent implements Content {
	private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

	private final String jsonSerialized;

	@SneakyThrows
	public JsonContent(Object jsonObject) {
		this.jsonSerialized = jsonObjectMapper.writeValueAsString(jsonObject);
	}

	@Override
	public String body() {
		return jsonSerialized;
	}

	@Override
	public String contentType() {
		return MimeTypes.JSON;
	}
}
