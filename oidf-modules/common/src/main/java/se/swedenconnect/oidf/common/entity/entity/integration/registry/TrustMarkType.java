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

package se.swedenconnect.oidf.common.entity.entity.integration.registry;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Domain value object for TrustMarkId
 *
 * @author Per Fredrik Plars
 */
@EqualsAndHashCode
public class TrustMarkType implements Serializable {

  private final static Pattern pattern = Pattern.compile("^(https?):\\/\\/[^\\s\\/:]+(:\\d+)?(\\/[^\\s]*)?$");
  @Getter
  final String trustMarkType;

  /**
   * Throws if trustMarkType does not follow regexp "^(https?):\\/\\/[^\\s\\/:]+(:\\d+)?(\\/[^\\s]*)?$"
   * @param trustMarkType trustMarkType
   */
  public TrustMarkType(final String trustMarkType) {
    this.trustMarkType = validateInternal(trustMarkType, IllegalArgumentException::new);
  }

  /**
   * Static method to create TrustMark
   * @param trustMarkType trustMarkType
   * @return TrustMarkId
   */
  public static TrustMarkType create(final String trustMarkType) {
    return new TrustMarkType(trustMarkType);
  }

  @Override
  public String toString() {
    return this.trustMarkType;
  }

  private static <EX extends Exception> String validateInternal(
      final String trustMarkType, final Function<String,EX> ex) throws EX{
    if(trustMarkType == null){
      throw ex.apply("Unable to create TrustMarkId since input is null.");
    }
    if (!pattern.matcher(trustMarkType).matches()) {
      throw ex.apply("Unable to create TrustMarkId. For input: '" + trustMarkType + "'");
    }
   return trustMarkType;
  }

  /**
   * Validated TrustMark, if it failes then exception is thrown according to ex
   * @param trustMarkType TrustMarkID
   * @param ex Function called to get an exception. A error string is supplied with the problem.
   * @return TrustMarkId
   * @param <EX> Exception
   * @throws EX  Exception thrown if trustmark is not validated
   */
  public static <EX extends Exception> TrustMarkType validate(
      final String trustMarkType, final Function<String,EX> ex) throws EX{
   return new TrustMarkType(validateInternal(trustMarkType,ex));
  }

}
