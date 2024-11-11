package se.digg.oidfed.test.metadata;

import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicyEntry;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.SubsetOfOperation;

import java.util.List;

public class MetadataPolicyFactory {

  public static MetadataPolicyEntry subset(final String grantTypes, final List<String> values) {
    final SubsetOfOperation subsetOfOperation = new SubsetOfOperation();
    subsetOfOperation.configure(values);
    return new MetadataPolicyEntry(grantTypes, List.of(subsetOfOperation));
  }
}
