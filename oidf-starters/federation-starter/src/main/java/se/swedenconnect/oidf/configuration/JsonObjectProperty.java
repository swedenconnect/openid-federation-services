/*
 * Copyright 2024-2025 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

/**
 * Adds a datatype for configuring a field as either a raw json string och file.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class JsonObjectProperty {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String json;
  private Resource resource;

  /**
   * Default constructor.
   */
  public JsonObjectProperty() {
  }

  /**
   * Constructor.
   * @param json to parse
   * @param resource containing json file
   */
  public JsonObjectProperty(@Nullable final String json, @Nullable final Resource resource) {
    this.json = json;
    this.resource = resource;
  }

  /**
   * @return this property as a json object
   */
  public Map<String, Object> toJsonObject() {
    if (Objects.nonNull(this.json)) {
      try {
        return OBJECT_MAPPER.readerFor(Map.class).readValue(this.json);
      } catch (final JsonProcessingException e) {
        throw new IllegalArgumentException("Configured JSON is not valid %s".formatted(this.json), e);
      }
    }
    if (Objects.nonNull(this.resource)) {
      try {
        final String contentAsString = this.resource.getContentAsString(Charset.defaultCharset());
        return OBJECT_MAPPER.readerFor(Map.class).readValue(contentAsString);
      } catch (final IOException e) {
        throw new IllegalArgumentException("Failed to read contents of resource", e);
      }
    }
    throw new IllegalArgumentException("Both json and resource is null");
  }

  /**
   * Validates that either of the fields is present
   */
  @PostConstruct
  public void validate() {
    if (Objects.isNull(this.json) && Objects.isNull(this.resource)) {
      throw new IllegalArgumentException("Both json and resource field is null");
    }
  }
}
