package se.digg.oidfed.trustmarkissuer.dvo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
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

  public TrustMarkId(final String trustMarkId) {

    if (!pattern.matcher(trustMarkId).matches()) {
      throw new IllegalArgumentException("Unable to create TrustMarkId. For input: '" + trustMarkId + "'");
    }
    this.trustMarkId = trustMarkId;
  }

  public static TrustMarkId create(final String trustMarkId) {
    return new TrustMarkId(trustMarkId);
  }

  @Override
  public String toString() {
    return trustMarkId;
  }
}
