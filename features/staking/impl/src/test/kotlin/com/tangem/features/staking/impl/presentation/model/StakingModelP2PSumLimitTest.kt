package com.tangem.features.staking.impl.presentation.model

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.tokens.model.Amount
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Model-level tests for the P2P ETH pool staking integration:
 * verifies that [StakingAnalyticsEvent.SumLimitError] is sent when the entered amount
 * exceeds the vault's computed maximum (= limit − totalAssets).
 *
 * Fixture:
 *   vault  address = "0xabc"  totalAssets = 5
 *   limit  "0xabc"  limit     = 10
 *   → maximum = 10 − 5 = 5.0  (scale=1, RoundingMode.FLOOR)
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelP2PSumLimitTest : StakingModelTestBase() {

    override val testIntegrationId: StakingIntegrationID = StakingIntegrationID.P2PEthPool

    private val vaultAddress = "0xabc"
    private val testVault = P2PEthPoolVault(
        vaultAddress = vaultAddress,
        displayName = "Test Vault",
        apy = BigDecimal("4.5"),
        baseApy = BigDecimal("4.0"),
        capacity = BigDecimal("1000"),
        totalAssets = BigDecimal("5"),
        feePercent = BigDecimal("0.1"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = false,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    // maximum = 10 − 5 = 5.0  (FLOOR scale=1)
    private val testLimits = mapOf(
        vaultAddress to VaultLimitInfo(limit = BigDecimal("10"), coefficient = null),
    )

    @BeforeEach
    fun setUpP2P() {
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns listOf(testVault)
        coEvery { p2pEthPoolRepository.getVaultLimitsSyncOrNull() } returns testLimits
    }

    /**
     * Helper: returns a [MutableStateFlow] whose value has [amountState] set to an
     * [AmountState.Data] mock with the given [cryptoAmountValue].
     * The flow is also wired to [stateController.uiState].
     */
    private fun stubUiStateWithCryptoAmount(cryptoAmountValue: BigDecimal): MutableStateFlow<StakingUiState> {
        val amountData = mockk<AmountState.Data>(relaxed = true) {
            every { amountTextField } returns mockk(relaxed = true) {
                every { cryptoAmount } returns Amount(
                    currencySymbol = "ETH",
                    value = cryptoAmountValue,
                    decimals = 18,
                )
            }
        }
        val uiStateFlow = MutableStateFlow(
            mockk<StakingUiState>(relaxed = true) {
                every { currentStep } returns StakingStep.InitialInfo
                every { amountState } returns amountData
            },
        )
        every { stateController.uiState } returns uiStateFlow
        return uiStateFlow
    }

    // ----- Test A ---------------------------------------------------------------

    @Test
    fun `GIVEN P2P vault max=5 WHEN amount 6 entered THEN SumLimitError analytics sent`() = runTest {
        stubUiStateWithCryptoAmount(BigDecimal("6"))

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountValueChange("6")

        verify {
            analyticsEventHandler.send(
                match { it is StakingAnalyticsEvent.SumLimitError }
            )
        }

        model.onDestroy()
    }

    // ----- Test B ---------------------------------------------------------------

    @Test
    fun `GIVEN P2P vault max=5 WHEN amount 4 entered THEN SumLimitError analytics NOT sent`() = runTest {
        stubUiStateWithCryptoAmount(BigDecimal("4"))

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountValueChange("4")

        verify(exactly = 0) {
            analyticsEventHandler.send(
                match { it is StakingAnalyticsEvent.SumLimitError }
            )
        }

        model.onDestroy()
    }
}