package com.tangem.data.txhistory.repository.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.txhistory.repository.TokenInfoRepository
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
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val userWallet = MockUserWalletFactory.create()
    private val mockCurrencyFactory = MockCryptoCurrencyFactory()

    private val factory = ExpressTransactionAssetFactory(
        multiAccountStatusListSupplier = multiAccountStatusListSupplier,
        userWalletsListRepository = userWalletsListRepository,
        tokenInfoRepository = tokenInfoRepository,
        excludedBlockchains = ExcludedBlockchains(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(tokenInfoRepository, multiAccountStatusListSupplier, userWalletsListRepository)
        every { multiAccountStatusListSupplier.invoke() } returns flowOf(emptyList())
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
        coEvery { tokenInfoRepository.getCachedTokens(any(), any()) } returns emptyMap()
    }

    @Test
    fun `GIVEN token only in cache WHEN create THEN builds token from cache`() = runTest {
        // Arrange
        val id = ExpressAsset.ID(networkId = "ethereum", contractAddress = TOKEN_CONTRACT)
        val cachedToken = mockCurrencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            id = "tether",
            contractAddress = TOKEN_CONTRACT,
        )
        coEvery { tokenInfoRepository.getCachedTokens(userWallet.walletId, any()) } returns mapOf(id to cachedToken)

        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result[ExpressLegKey(id, PAYOUT_ADDRESS)]).isEqualTo(cachedToken)
    }

    @Test
    fun `GIVEN coin asset WHEN create THEN builds coin`() = runTest {
        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result[ExpressLegKey(COIN_ASSET_ID, FROM_ADDRESS)]).isInstanceOf(CryptoCurrency.Coin::class.java)
    }

    @Test
    fun `GIVEN token absent from portfolio and cache WHEN create THEN omits it`() = runTest {
        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        val tokenId = ExpressAsset.ID(networkId = "ethereum", contractAddress = TOKEN_CONTRACT)
        assertThat(result).doesNotContainKey(ExpressLegKey(tokenId, PAYOUT_ADDRESS))
    }

    @Test
    fun `GIVEN two copies of the coin on different derivations WHEN create THEN picks the one at the leg address`() =
        runTest {
            // Arrange — the same coin held by two accounts; the deal's pay-in address belongs to the regular one.
            val regular = ethereumCoin(isCustom = false)
            val custom = ethereumCoin(isCustom = true)
            givenPortfolio(
                statusAt(custom, address = "other-account-address"),
                statusAt(regular, address = FROM_ADDRESS),
            )

            // Act
            val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

            // Assert
            assertThat(result[ExpressLegKey(COIN_ASSET_ID, FROM_ADDRESS)]).isEqualTo(regular)
        }

    @Test
    fun `GIVEN leg address in another casing WHEN create THEN still picks the holding copy`() = runTest {
        // Arrange — the backend reports the provider's casing, the wallet derives EVM addresses checksummed.
        val regular = ethereumCoin(isCustom = false)
        givenPortfolio(statusAt(regular, address = FROM_ADDRESS.uppercase()))

        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result[ExpressLegKey(COIN_ASSET_ID, FROM_ADDRESS)]).isEqualTo(regular)
    }

    @Test
    fun `GIVEN no account holds the leg address WHEN create THEN falls back to any matching copy`() = runTest {
        // Arrange — a send-and-swap payout, or addresses not loaded yet: any copy still carries the name and icon.
        val custom = ethereumCoin(isCustom = true)
        givenPortfolio(statusAt(custom, address = "some-other-address"))

        // Act
        val result = factory.create(userWallet.walletId, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result[ExpressLegKey(COIN_ASSET_ID, FROM_ADDRESS)]).isEqualTo(custom)
    }

    private fun givenPortfolio(vararg statuses: CryptoCurrencyStatus) {
        val accountStatusList = mockk<AccountStatusList> {
            every { flattenPersonalCurrencies() } returns statuses.toList()
        }
        every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(accountStatusList))
    }

    /** Only the leg's address matters for the resolution under test, so the rest of the status is left unstubbed. */
    private fun statusAt(currency: CryptoCurrency, address: String): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = mockk {
            every { networkAddress } returns NetworkAddress.Single(
                NetworkAddress.Address(value = address, type = NetworkAddress.Address.Type.Primary),
            )
        },
    )

    /** The Ethereum coin as one account holds it — [isCustom] `true` is the custom-derivation copy of another account. */
    private fun ethereumCoin(isCustom: Boolean): CryptoCurrency.Coin {
        val coin = mockCurrencyFactory.createCoin(blockchain = Blockchain.Ethereum)
        if (!isCustom) return coin
        val derivationPath = Network.DerivationPath.Custom(value = "m/44'/60'/1'/0/0")
        return coin.copy(
            network = coin.network.copy(
                id = coin.network.id.copy(derivationPath = derivationPath),
                derivationPath = derivationPath,
            ),
            isCustom = true,
        )
    }

    /** A swap from a native coin (empty contract) to an Ethereum token. */
    private fun coinToTokenSwap() = ExpressExchangeEntity(
        txId = "tx-1",
        providerId = "provider",
        fromAddress = FROM_ADDRESS,
        payinAddress = "payin-addr",
        payinExtraId = null,
        payoutAddress = PAYOUT_ADDRESS,
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
        const val FROM_ADDRESS = "0xownAddress1234"
        const val PAYOUT_ADDRESS = "payout-addr"
        val COIN_ASSET_ID = ExpressAsset.ID(
            networkId = "ethereum",
            contractAddress = ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE,
        )
    }
}