package com.tangem.tests.e2e

import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.grow.datasource.config.Express
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.E2eCatalog
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.scenarios.addNetworkToMainViaManageTokens
import com.tangem.scenarios.assertTokenBalanceLoadedAndNonNegative
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.resetE2eWalletTokens
import com.tangem.scenarios.waitForMainScreenLoaded
import com.tangem.scenarios.revealTokenOnMain
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AddNetworkViaManageTokensE2ETest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(TangemGatewayApiConfig.TANGEM_TECH_KEY, ApiEnvironment.PROD),
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD),
    )
    @AllureId("2076")
    @DisplayName("E2E: add network to main via Manage Tokens")
    @Test
    fun addNetworkViaManageTokensTest() {
        val network = E2eCatalog.network
        setupHooks(useWireMock = false).run {
            step("Reset the E2E wallet's backend token list") {
                resetE2eWalletTokens(E2eCatalog.WALLET_ID)
            }
            step("Import the E2E hot wallet from seed phrase") {
                openMainScreenWithExistingHotWallet(E2eCatalog.SEED_PHRASE)
            }
            waitForMainScreenLoaded()
            step("Add the '${network.networkName}' network via Manage Tokens") {
                addNetworkToMainViaManageTokens(network)
            }
            step("Reveal '${network.mainScreenToken}' on main") {
                revealTokenOnMain(network.mainScreenToken)
            }
            step("Assert '${network.mainScreenToken}' balance is loaded and >= 0") {
                assertTokenBalanceLoadedAndNonNegative(network.mainScreenToken)
            }
        }
    }
}