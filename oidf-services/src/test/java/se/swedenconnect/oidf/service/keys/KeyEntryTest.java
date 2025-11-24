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
package se.swedenconnect.oidf.service.keys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KeyEntryTest {
  @Test
  void testParseKey() {
    final String key = """
        ewogICAgICAgICJhbGciOiAiRVM1MTIiLAogICAgICAgICJjcnYiOiAiUC01MjEiLAogICAgICAgICJraWQiOiAiQjRkbGU1ajJYT19yLXZsdG9qSDB6X0FHTEhUSjVTamR0di04MDA1YzRMNCIsCiAgICAgICAgImt0eSI6ICJFQyIsCiAgICAgICAgInVzZSI6ICJzaWciLAogICAgICAgICJ4IjogIkFJMXRINm5qRlRBT1hiVDFQVkp3QS1VaWh1R3dwdk5HX1BYWm50R1lIM0o4QzFDcjd2MmZiVkxyM1l4VnR3bW10cGZsZWVoN3dxUWtndWdRWm1iVjV6T3kiLAogICAgICAgICJ5IjogIkFFdTNoa1NycTVHZVN5ZW5rUkxfR180QkJlVXRpLXV5ZDVOQzBiZmlkYlR2VnBkdXZVTHVHSGV2QXRZUUFmUnJYYTlOekFkTHVJQkFpbWFXbWNlLTBmc2wiCiAgICAgIH0K
        """;
    final String pem = """
        -----BEGIN CERTIFICATE-----
        MIIFGTCCAwGgAwIBAgIUMACsMXZsd7j+wAd4XJETj39Z8jYwDQYJKoZIhvcNAQEL
        BQAwHDEaMBgGA1UEAwwRb2lkZl9yZWdpc3RyeV9rZXkwHhcNMjUxMTE5MjAzOTMw
        WhcNMzUxMTE3MjAzOTMwWjAcMRowGAYDVQQDDBFvaWRmX3JlZ2lzdHJ5X2tleTCC
        AiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAJZ/OISQt6AjUMN/UQKHVqPb
        AbPsZxDzCWQMDHybMwIHU2UVjsPuTLMIrFL4PvpYk/eEE+YNAMpPAKZUYH1WUBZ7
        wFeZh4v7F8asALruE1CD+QCXq8NT6ofv0jjrYwyarwfySq7S/Z6FS155l6Ju82AQ
        vrwG8BzyMdUkTTw43CDjOTEKUXu4sz4MS3htqQQpHtJ86js3oA9d5cuelpTmz1e4
        h38dbEHLqiLpDLaWO+jvGhKJy9QrMCQ/ww6ttvjhpHWZJ6defoD1+zbT5vnp7I8T
        w9SeLdxTYndtPOT8BY811r1kCDrBxNV7S9yvntGQP9+7dKvL0ane7833i3dFc6Wj
        K+iAerUEIfTTSR0JTj2NKdckM2kbjglPKYqq15ChQvJ4g4+UfH92A1JyEgbPp3oT
        nWHDpOIgxqH3SfIM0Stc2tzeQt/WZdgzK+RcYCuAV0hbnkrbYxn59BOxVXgF5kvU
        6XSV0IdW69FNAe4HsEK2DGpbfkQLhsjtVF5Gn8nxTeg1b1zY7HzE/IsbonO7X/G3
        6YVS/3w1H2OfoWueZUMLOPjpASum3lNfsu7ayRoJ+MPWdNx0Ogg1e/WpfdWkrm9u
        sumqhz/eMrExoLfBie/er5ownmWXwQyTjq251op2n7v6J/dUFUdCEb1OLfC2RGFu
        +G7+462CJj32qNjyggWVAgMBAAGjUzBRMB0GA1UdDgQWBBR+tnA+b5nkHKEvoDul
        TQnLiLWg+TAfBgNVHSMEGDAWgBR+tnA+b5nkHKEvoDulTQnLiLWg+TAPBgNVHRMB
        Af8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4ICAQBPDFjdZg5iKaDTeHerXtJ1q8KV
        jnVnBFnBZ7t3i+3OscmTpSyGQcAnLiKIKw7n9O/p4kX0AZV+4o1/FDnr5dZMQepd
        LyLq9z0EX6OWTHf1IMzvqqCGJxIhLG4Lu1dk1YW5gieqMwku7QK1cJoexMK0JyS6
        y3GOWewgRvFaAw7pIjn1q2Yp1WBsA4b7QQU+xTJleKuYXHVI2Tnx4RFLVILWudio
        htk65j99I+z/LGM18QEjFEn61uRx2la+lxo0UYdbmv1853amnVuSixCsriCvxI8s
        SRWdbbLu1Vw1Ci2FSdyID5oM2tTWecgA1aV7XDus/3VaF6RpMXygj6rUQAWTw1ws
        Ut807ZwHNbeAJjvJcbT//o1tr3z3SE20sXYoHz2thz8MHo2ucI2TqrZIHO1Jbc5x
        mTyc2rPZ2IpfhwfLhl/Ov0K+wzx49DgIcJ+MQXZFUf1tAcjKVJBtkJs2G9ml/xca
        wnN9ESIyhW/0E3sn7V3c8gianIAxK7O7HN2T6SwJo+h+ggLqpzJffcoT14SX4NCh
        UxDL5ZB/7T50qbovr7r2AlbtyHMGggofFsvBuUO7F2LeLhjFFmJqmCw9SFsTu/bJ
        l4bj5m0ZH24FHmFSFDI2+ytu1m68xkpUcIV0RMKV5stt+K58q0hXv64Ua1rbNovb
        nSHLYt/MgpL7yoZFVQ==
        -----END CERTIFICATE-----""";

    final KeyEntry parsedKey = new KeyEntry("name", key,null);
    Assertions.assertEquals("B4dle5j2XO_r-vltojH0z_AGLHTJ5Sjdtv-8005c4L4", parsedKey.getKey().getKeyID());

    Assertions.assertEquals("_ex9CH8rrUNrheFsZrpbsqctsnwusja404CIGe4-Q9Y",new KeyEntry("name",null,pem).getKey().getKeyID());
  }

}