package com.tangem.tests.e2e

import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.grow.datasource.config.Express
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.E2eCatalog
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.scenarios.addTokenToMainViaManageTokens
import com.tangem.scenarios.addTokenToSecondAccountViaManageTokens
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.resetE2eWalletTokens
import com.tangem.scenarios.sendTokenFromRicherAccountToOther
import com.tangem.scenarios.waitForMainScreenLoaded
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class SendTokenBetweenAccountsE2ETest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(TangemGatewayApiConfig.TANGEM_TECH_KEY, ApiEnvironment.PROD),
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD),
    )
    @AllureId("13953")
    @DisplayName("E2E: send a token from the richer account to the other")
    @Test
    fun sendTokenFromRicherAccountToOtherTest() {
        val network = E2eCatalog.network
        val token = requireNotNull(network.token) {
            "The token Send test needs a token in the catalog (qa-tools network.token)"
        }
        setupHooks(useWireMock = false).run {
            assertEquals(
                "The Send test needs two account addresses from the catalog (qa-tools defaultAddress + customAddress)",
                2,
                network.accountAddresses.size,
            )
            step("Reset the E2E wallet's backend token list") {
                resetE2eWalletTokens(E2eCatalog.WALLET_ID)
            }
            step("Import the E2E hot wallet from seed phrase") {
                openMainScreenWithExistingHotWallet(E2eCatalog.SEED_PHRASE)
            }
            waitForMainScreenLoaded()
            step("Add '${token.tokenName}' to the main account") {
                addTokenToMainViaManageTokens(token)
            }
            step("Add '${token.tokenName}' to the second account") {
                addTokenToSecondAccountViaManageTokens(token)
            }
            step("Send '${token.sendAmount}' ${token.searchQuery} from the richer account to the other") {
                sendTokenFromRicherAccountToOther(network, token)
            }
        }
    }
}