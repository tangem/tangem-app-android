package com.tangem.tests.hotWallet

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.SEED_PHRASE_15
import com.tangem.common.constants.TestConstants.SEED_PHRASE_18
import com.tangem.common.constants.TestConstants.SEED_PHRASE_21
import com.tangem.common.constants.TestConstants.SEED_PHRASE_24
import com.tangem.common.utils.checkServiceHealth
import com.tangem.common.utils.getAddressesFromApi
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.verifyAddresses
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertNotNull
import org.junit.Test

@HiltAndroidTest
class AddressesTest : BaseTestCase() {

    private var apiAddressesJson: String? = null

    private fun setupAddressTestHooks(seedKey: String) = setupHooks(
        additionalBeforeAppLaunchSection = {
            val status = checkServiceHealth()
            assertNotNull("QA tools service is unreachable ([REDACTED_ENV_URL]", status)

            apiAddressesJson = getAddressesFromApi(seedKey)
            assertNotNull("Failed to fetch reference addresses for '$seedKey'", apiAddressesJson)
        },
    )

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("1792")
    @DisplayName("Hot wallet: auto derivation addresses for seed 12")
    @Test
    fun seed12AddressesTest() {
        setupAddressTestHooks("twelve").run {
            verifyAddresses(SEED_PHRASE_12, requireNotNull(apiAddressesJson))
        }
    }

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("5106")
    @DisplayName("Hot wallet: auto derivation addresses for seed 15")
    @Test
    fun seed15AddressesTest() {
        setupAddressTestHooks("fifteen").run {
            verifyAddresses(SEED_PHRASE_15, requireNotNull(apiAddressesJson))
        }
    }

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("5107")
    @DisplayName("Hot wallet: auto derivation addresses for seed 18")
    @Test
    fun seed18AddressesTest() {
        setupAddressTestHooks("eighteen").run {
            verifyAddresses(SEED_PHRASE_18, requireNotNull(apiAddressesJson))
        }
    }

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("5108")
    @DisplayName("Hot wallet: auto derivation addresses for seed 21")
    @Test
    fun seed21AddressesTest() {
        setupAddressTestHooks("twenty_one").run {
            verifyAddresses(SEED_PHRASE_21, requireNotNull(apiAddressesJson))
        }
    }

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("5109")
    @DisplayName("Hot wallet: auto derivation addresses for seed 24")
    @Test
    fun seed24AddressesTest() {
        setupAddressTestHooks("twenty_four").run {
            verifyAddresses(SEED_PHRASE_24, requireNotNull(apiAddressesJson))
        }
    }
}