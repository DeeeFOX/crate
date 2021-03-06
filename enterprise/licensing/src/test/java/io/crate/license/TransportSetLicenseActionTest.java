/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.license;

import io.crate.test.integration.CrateUnitTest;
import org.elasticsearch.cluster.metadata.MetaData;
import org.junit.Test;

import static io.crate.integrationtests.LicenseITest.ENTERPRISE_LICENSE_KEY;
import static io.crate.license.EnterpriseLicenseService.UNLIMITED_EXPIRY_DATE_IN_MS;

public class TransportSetLicenseActionTest extends CrateUnitTest {

    @Test
    public void testTrialLicenseCannotOverrideExistingLicense() throws Exception {
        MetaData currentMetaData = MetaData.builder()
            .putCustom(LicenseKey.WRITEABLE_TYPE, new LicenseKey(ENTERPRISE_LICENSE_KEY))
            .build();

        LicenseData licenseData = new LicenseData(
            UNLIMITED_EXPIRY_DATE_IN_MS,
            "Trial-Dummy",
            3
        );
        LicenseKey trialLicenseKey = TrialLicense.createLicenseKey(LicenseKey.VERSION, licenseData);

        assertTrue(TransportSetLicenseAction.ignoreNewTrialLicense(trialLicenseKey, currentMetaData));
    }
}
