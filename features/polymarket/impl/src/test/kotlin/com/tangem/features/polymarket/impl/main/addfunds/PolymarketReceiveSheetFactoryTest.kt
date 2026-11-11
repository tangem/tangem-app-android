package com.tangem.features.polymarket.impl.main.addfunds

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositAsset
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketReceiveSheetFactoryTest {

    private val collateral: CryptoCurrency.Token = mockk()
    private val polygonUsdc: CryptoCurrency.Token = mockk()
    private val solanaUsdc: CryptoCurrency.Token = mockk()

    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory = mockk {
        every { createDepositTarget(PolymarketDepositAsset.POLYGON_USDC) } returns polygonUsdc
        every { createDepositTarget(SOLANA_USDC) } returns solanaUsdc
    }

    @Test
    fun `GIVEN phase two disabled WHEN created THEN the Polygon address opens at once in the collateral`() {
        // Arrange
        val factory = createFactory(isPhaseTwoEnabled = false)

        // Act
        val actual = factory.create(collateral = collateral, userWalletId = USER_WALLET_ID, requisites = READY)

        // Assert
        val config = (actual as PolymarketFundingSheetConfig.Receive).config
        assertThat(config.cryptoCurrency).isEqualTo(collateral)
        assertThat(config.receiveAddress.map { it.value }).containsExactly(EVM_ADDRESS)
    }

    @Test
    fun `GIVEN phase two enabled WHEN created THEN every network is offered in catalogue order`() {
        // Arrange
        val factory = createFactory(isPhaseTwoEnabled = true)

        // Act
        val actual = factory.create(collateral = collateral, userWalletId = USER_WALLET_ID, requisites = READY)

        // Assert
        val configs = (actual as PolymarketFundingSheetConfig.ChooseNetwork).receiveConfigs
        assertThat(configs.map { it.cryptoCurrency }).containsExactly(polygonUsdc, solanaUsdc).inOrder()
    }

    @Test
    fun `GIVEN phase two enabled WHEN created THEN each network carries its family address and minimum`() {
        // Arrange
        val factory = createFactory(isPhaseTwoEnabled = true)

        // Act
        val actual = factory.create(collateral = collateral, userWalletId = USER_WALLET_ID, requisites = READY)

        // Assert
        val configs = (actual as PolymarketFundingSheetConfig.ChooseNetwork).receiveConfigs
        assertThat(configs.map { it.receiveAddress.single().value }).containsExactly(EVM_ADDRESS, SVM_ADDRESS)
            .inOrder()
        assertThat(configs.map { it.tokenReceiveNotification.single().formatArgs })
            .containsExactly(listOf("$2"), listOf("$5"))
            .inOrder()
    }

    private fun createFactory(isPhaseTwoEnabled: Boolean) = PolymarketReceiveSheetFactory(
        featureToggles = mockk<PolymarketFeatureToggles> {
            every { isPolymarketPhaseTwoEnabled } returns isPhaseTwoEnabled
        },
        collateralCurrencyFactory = collateralCurrencyFactory,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        const val EVM_ADDRESS = "0xbridge"
        const val SVM_ADDRESS = "SoLbridge"

        val SOLANA_USDC = PolymarketDepositAsset.CATALOG.single { it.family == PolymarketChainFamily.SVM }

        val REQUISITES = PolymarketDepositRequisites(
            networks = listOf(
                PolymarketDepositNetwork(
                    chainId = PolymarketContracts.CHAIN_ID.toString(),
                    chainName = "Polygon",
                    family = PolymarketChainFamily.EVM,
                    address = EVM_ADDRESS,
                    tokens = listOf(
                        PolymarketDepositToken(
                            symbol = "USDC",
                            address = PolymarketContracts.USDC_TOKEN_ADDRESS,
                            decimals = 6,
                            minCheckoutUsd = BigDecimal(2),
                        ),
                    ),
                ),
                PolymarketDepositNetwork(
                    chainId = SOLANA_USDC.chainId,
                    chainName = "Solana",
                    family = PolymarketChainFamily.SVM,
                    address = SVM_ADDRESS,
                    tokens = listOf(
                        PolymarketDepositToken(
                            symbol = "USDC",
                            address = SOLANA_USDC.contractAddress,
                            decimals = 6,
                            minCheckoutUsd = BigDecimal(5),
                        ),
                    ),
                ),
            ),
        )

        val READY = RequisitesState.Ready(target = requireNotNull(REQUISITES.default), options = REQUISITES.options)
    }
}