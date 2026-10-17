package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositAsset
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetPredictionDepositCurrencyStatusesUseCaseTest {

    private val requisitesStore: PredictionDepositRequisitesStore = mockk()
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier = mockk()
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory = mockk()

    private val useCase = GetPredictionDepositCurrencyStatusesUseCase(
        requisitesStore = requisitesStore,
        predictionAccountStatusSupplier = predictionAccountStatusSupplier,
        collateralCurrencyFactory = collateralCurrencyFactory,
    )

    private val polygonToken: CryptoCurrency.Token = mockk()
    private val solanaToken: CryptoCurrency.Token = mockk()
    private val polygonStatus: CryptoCurrencyStatus = mockk()
    private val solanaStatus: CryptoCurrencyStatus = mockk()
    private val active: PredictionAccountStatusValue.Active = mockk {
        every { depositStatus(polygonToken, EVM_ADDRESS) } returns polygonStatus
        every { depositStatus(solanaToken, SVM_ADDRESS) } returns solanaStatus
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(requisitesStore, predictionAccountStatusSupplier, collateralCurrencyFactory)
        every { predictionAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(active)
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns PolymarketDepositRequisites(
            networks = listOf(listed(SOLANA_USDC, SVM_ADDRESS), listed(PolymarketDepositAsset.POLYGON_USDC, EVM_ADDRESS)),
        )
        every { collateralCurrencyFactory.createDepositTarget(PolymarketDepositAsset.POLYGON_USDC) } returns polygonToken
        every { collateralCurrencyFactory.createDepositTarget(SOLANA_USDC) } returns solanaToken
    }

    @Test
    fun `GIVEN an active account and requisites WHEN invoke THEN each option sits at its family address in catalogue order`() =
        runTest {
            // Act
            val actual = useCase(USER_WALLET_ID)

            // Assert
            assertThat(actual).containsExactly(polygonStatus, solanaStatus).inOrder()
        }

    @Test
    fun `GIVEN the status is still loading WHEN invoke THEN waits for the active account`() = runTest {
        // Arrange
        every { predictionAccountStatusSupplier(USER_WALLET_ID) } returns
            flowOf(PredictionAccountStatusValue.Loading, active)

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).containsExactly(polygonStatus, solanaStatus).inOrder()
    }

    @Test
    fun `GIVEN the status never leaves loading WHEN invoke THEN gives up and returns empty`() = runTest {
        // Arrange
        every { predictionAccountStatusSupplier(USER_WALLET_ID) } returns flow {
            emit(PredictionAccountStatusValue.Loading)
            awaitCancellation()
        }

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN the account is not active WHEN invoke THEN returns empty`() = runTest {
        // Arrange
        every { predictionAccountStatusSupplier(USER_WALLET_ID) } returns
            flowOf(mockk<PredictionAccountStatusValue.Onboarded>())

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN no requisites this session WHEN invoke THEN returns empty`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun listed(asset: PolymarketDepositAsset, address: String) = PolymarketDepositNetwork(
        chainId = asset.chainId,
        chainName = asset.blockchain.name,
        family = requireNotNull(PolymarketChainFamily.of(asset.chainId)),
        address = address,
        tokens = listOf(
            PolymarketDepositToken(
                symbol = "USDC",
                address = asset.contractAddress,
                decimals = asset.decimals,
                minCheckoutUsd = BigDecimal("2"),
            ),
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val SOLANA_USDC = PolymarketDepositAsset.CATALOG.first { it.blockchain == Blockchain.Solana }
        const val EVM_ADDRESS = "0xbridge"
        const val SVM_ADDRESS = "So1Bridge"
    }
}