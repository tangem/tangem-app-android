package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultPredictionDepositRequisitesStoreTest {

    private val store = DefaultPredictionDepositRequisitesStore()

    @Test
    fun `GIVEN nothing stored WHEN get collected THEN it emits null at once`() = runTest {
        // Arrange
        val flow = store.get(userWalletId = USER_WALLET_ID)

        // Act
        val actual = flow.first()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a value stored for one wallet WHEN another wallet is read THEN it stays empty`() = runTest {
        // Arrange
        store.store(userWalletId = USER_WALLET_ID, value = REQUISITES)

        // Act
        val actual = store.getSyncOrNull(userWalletId = OTHER_USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a stored value WHEN cleared THEN it is gone`() = runTest {
        // Arrange
        store.store(userWalletId = USER_WALLET_ID, value = REQUISITES)

        // Act
        store.clear(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(store.getSyncOrNull(userWalletId = USER_WALLET_ID)).isNull()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        val OTHER_USER_WALLET_ID = UserWalletId(stringValue = "022")

        val REQUISITES = createRequisites()

        fun createRequisites(networks: List<PolymarketDepositNetwork> = listOf(createDepositNetwork())) =
            PolymarketDepositRequisites(networks = networks)

        fun createDepositNetwork(
            chainId: String = "137",
            chainName: String = "Polygon",
            family: PolymarketChainFamily = PolymarketChainFamily.EVM,
            address: String = "0x4038A7a1",
            tokens: List<PolymarketDepositToken> = listOf(createDepositToken()),
        ) = PolymarketDepositNetwork(
            chainId = chainId,
            chainName = chainName,
            family = family,
            address = address,
            tokens = tokens,
        )

        fun createDepositToken(
            symbol: String = "USDC",
            address: String = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359",
            decimals: Int = 6,
            minCheckoutUsd: BigDecimal = BigDecimal("2"),
        ) = PolymarketDepositToken(
            symbol = symbol,
            address = address,
            decimals = decimals,
            minCheckoutUsd = minCheckoutUsd,
        )
    }
}