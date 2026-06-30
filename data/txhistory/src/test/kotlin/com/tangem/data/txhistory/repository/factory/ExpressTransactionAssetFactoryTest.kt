package com.tangem.data.txhistory.repository.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.TokenInfoEntity
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressTransactionAssetFactoryTest {

    private val tokenInfoRepository: TokenInfoRepository = mockk()
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val userWallet = MockUserWalletFactory.create()

    private val factory = ExpressTransactionAssetFactory(
        multiAccountListSupplier = multiAccountListSupplier,
        userWalletsListRepository = userWalletsListRepository,
        tokenInfoRepository = tokenInfoRepository,
        excludedBlockchains = ExcludedBlockchains(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(tokenInfoRepository, multiAccountListSupplier, userWalletsListRepository)
        every { multiAccountListSupplier.invoke() } returns flowOf(emptyList())
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
        coEvery { tokenInfoRepository.getCached(any()) } returns emptyList()
    }

    @Test
    fun `GIVEN token only in cache WHEN create THEN builds token from cache`() = runTest {
        // Arrange
        coEvery { tokenInfoRepository.getCached(any()) } returns listOf(
            TokenInfoEntity(
                networkId = "ethereum",
                contractAddress = TOKEN_CONTRACT,
                coinId = "tether",
                name = "Tether",
                symbol = "USDT",
                decimals = 6,
                updatedAt = 0,
            ),
        )

        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        val token = result[ExpressAsset.ID(networkId = "ethereum", contractAddress = TOKEN_CONTRACT)]
        assertThat(token).isInstanceOf(CryptoCurrency.Token::class.java)
        assertThat((token as CryptoCurrency.Token).symbol).isEqualTo("USDT")
    }

    @Test
    fun `GIVEN coin asset WHEN create THEN builds coin`() = runTest {
        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        val coin = result[ExpressAsset.ID(networkId = "ethereum", contractAddress = ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE)]
        assertThat(coin).isInstanceOf(CryptoCurrency.Coin::class.java)
    }

    @Test
    fun `GIVEN token absent from portfolio and cache WHEN create THEN omits it`() = runTest {
        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result).doesNotContainKey(ExpressAsset.ID(networkId = "ethereum", contractAddress = TOKEN_CONTRACT))
    }

    /** A swap from a native coin (empty contract) to an Ethereum token. */
    private fun coinToTokenSwap() = ExpressExchangeEntity(
        txId = "tx-1",
        ownerAddress = "owner",
        providerId = "provider",
        fromAddress = "owner",
        payinAddress = "payin-addr",
        payinExtraId = null,
        payoutAddress = "payout-addr",
        refundAddress = null,
        refundExtraId = null,
        rateType = "float",
        status = "finished",
        externalTxId = null,
        externalTxUrl = null,
        payinHash = "payin",
        payoutHash = "payout",
        refundNetwork = null,
        refundContractAddress = null,
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-01T00:00:00Z",
        payTill = null,
        averageDuration = null,
        from = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE,
            network = "ethereum",
            decimals = 18,
            amount = "1.0",
            actualAmount = null,
        ),
        to = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = TOKEN_CONTRACT,
            network = "ethereum",
            decimals = 6,
            amount = "100.0",
            actualAmount = null,
        ),
    )

    private companion object {
        const val TOKEN_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
}