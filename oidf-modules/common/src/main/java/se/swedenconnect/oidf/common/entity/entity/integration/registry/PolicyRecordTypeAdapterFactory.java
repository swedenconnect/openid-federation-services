/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.common.entity.entity.integration.registry;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.TypeAdapterFactory;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.nimbusds.jose.shaded.gson.stream.JsonReader;
import com.nimbusds.jose.shaded.gson.stream.JsonWriter;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;

import java.io.IOException;
import java.util.Map;

/**
 * Gson type adapter factory for {@link PolicyRecord}. JSON null values inside the policy are written, since a
 * metadata policy operator may have the value null (for example {@code {"value": null}}, which removes a parameter).
 * All other output is the same as Gson's default serialization of the record. Reading is unchanged.
 *
 * <p>
 * Nulls are only kept when the record is written in streaming mode. A {@code JsonSerializer} that serializes the
 * record into a tree, which is then written by a writer that does not serialize nulls, will lose them.
 * </p>
 *
 * @author Martin Lindström
 */
public class PolicyRecordTypeAdapterFactory implements TypeAdapterFactory {

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    if (!PolicyRecord.class.equals(type.getRawType())) {
      return null;
    }
    final TypeAdapter<PolicyRecord> delegate = gson.getDelegateAdapter(this, TypeToken.get(PolicyRecord.class));
    final TypeAdapter<Map<String, Object>> policyAdapter = gson.getAdapter(new TypeToken<>() {
    });
    return (TypeAdapter<T>) new PolicyRecordTypeAdapter(delegate, policyAdapter);
  }

  /**
   * Type adapter writing a policy record with the nulls of the policy kept.
   *
   * @author Martin Lindström
   */
  private static class PolicyRecordTypeAdapter extends TypeAdapter<PolicyRecord> {

    private final TypeAdapter<PolicyRecord> delegate;
    private final TypeAdapter<Map<String, Object>> policyAdapter;

    /**
     * Constructor.
     *
     * @param delegate Gson's default adapter, used for reading
     * @param policyAdapter adapter for the policy map
     */
    PolicyRecordTypeAdapter(final TypeAdapter<PolicyRecord> delegate,
        final TypeAdapter<Map<String, Object>> policyAdapter) {
      this.delegate = delegate;
      this.policyAdapter = policyAdapter;
    }

    /** {@inheritDoc} */
    @Override
    public void write(final JsonWriter out, final PolicyRecord value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.beginObject();
      if (value.getId() != null) {
        out.name("id").value(value.getId());
      }
      if (value.getPolicy() != null) {
        out.name("policy");
        final boolean serializeNulls = out.getSerializeNulls();
        out.setSerializeNulls(true);
        try {
          this.policyAdapter.write(out, value.getPolicy());
        }
        finally {
          out.setSerializeNulls(serializeNulls);
        }
      }
      out.endObject();
    }

    /** {@inheritDoc} */
    @Override
    public PolicyRecord read(final JsonReader in) throws IOException {
      return this.delegate.read(in);
    }
  }
}
