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
package se.swedenconnect.oidf.service.submodule;

import lombok.Getter;

import java.io.Serializable;

/**
 * Entity containing request and response.
 *
 * @author Felix Hellman
 */
@Getter
public class RequestResponseEntry implements Serializable {
  private final String request;
  private final String response;

  /**
   * Constructor.
   * @param request
   * @param response
   */
  public RequestResponseEntry(final String request,
                              final String response) {
    this.request = request;
    this.response = response;
  }
}
