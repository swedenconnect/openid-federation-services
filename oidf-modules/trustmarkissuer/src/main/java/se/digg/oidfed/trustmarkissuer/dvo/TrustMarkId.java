/*
 * Copyright 2024 Sweden Connect
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
 */

package se.digg.oidfed.trustmarkissuer.dvo;

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
public class TrustMarkId implements Serializable {

  private final static Pattern pattern = Pattern.compile("^(https?):\\/\\/[^\\s\\/:]+(:\\d+)?(\\/[^\\s]*)?$");
  @Getter
  final String trustMarkId;

  /**
   * Throws if trustMarkId does not follow regexp "^(https?):\\/\\/[^\\s\\/:]+(:\\d+)?(\\/[^\\s]*)?$"
   * @param trustMarkId trustMarkId
   */
  public TrustMarkId(final String trustMarkId) {
    this.trustMarkId = validateInternal(trustMarkId, IllegalArgumentException::new);
  }

  /**
   * Static method to create TrustMark
   * @param trustMarkId trustMarkId
   * @return TrustMarkId
   */
  public static TrustMarkId create(final String trustMarkId) {
    return new TrustMarkId(trustMarkId);
  }

  @Override
  public String toString() {
    return trustMarkId;
  }



  private static <EX extends Exception> String validateInternal(String trustMarkId, Function<String,EX> ex) throws EX{
    if(trustMarkId == null){
      throw ex.apply("Unable to create TrustMarkId since input is null.");
    }
    if (!pattern.matcher(trustMarkId).matches()) {
      throw ex.apply("Unable to create TrustMarkId. For input: '" + trustMarkId + "'");
    }
   return trustMarkId;
  }

  /**
   * Validated TrustMark, if it failes then exception is thrown according to ex
   * @param trustMarkId TrustMarkID
   * @param ex Function called to get an exception. A error string is supplied with the problem.
   * @return TrustMarkId
   * @param <EX> Exception
   * @throws EX  Exception thrown if trustmark is not validated
   */
  public static <EX extends Exception> TrustMarkId validate(String trustMarkId, Function<String,EX> ex) throws EX{
   return new TrustMarkId(validateInternal(trustMarkId,ex));
  }

}
