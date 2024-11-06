package se.digg.oidfed.trustmarkissuer.dvo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Domain value object for EntityId
 *
 * @author Per Fredrik Plars
 */
@EqualsAndHashCode
public class EntityId implements Serializable {

  private final static Pattern pattern = Pattern.compile("^(https?):\\/\\/[^\\s\\/:]+(:\\d+)?(\\/[^\\s]*)?$");
  @Getter
  final String entityid;

  public EntityId(final String entityid) {

    if (!pattern.matcher(entityid).matches()) {
      throw new IllegalArgumentException("Unable to create entityid. For input: '" + entityid + "'");
    }
    this.entityid = entityid;
  }

  public static EntityId create(final String entityid) {
    return new EntityId(entityid);
  }

  @Override
  public String toString() {
    return entityid;
  }
}
