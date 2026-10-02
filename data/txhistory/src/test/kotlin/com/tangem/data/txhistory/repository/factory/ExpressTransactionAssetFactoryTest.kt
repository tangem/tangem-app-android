package com.tangem.data.txhistory.repository.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.mock.MockAccounts
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
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val userWallet = MockUserWalletFactory.create()
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val cachedToken = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Ethereum,
        id = "tether",
        contractAddress = TOKEN_CONTRACT,
    )

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
        coEvery { tokenInfoRepository.getCachedTokens(any(), any()) } returns emptyMap()
    }

    @Test
    fun `GIVEN token only in cache WHEN create THEN builds token from cache`() = runTest {
        // Arrange
        val id = ExpressAsset.ID(networkId = "ethereum", contractAddress = TOKEN_CONTRACT)
        coEvery { tokenInfoRepository.getCachedTokens(userWallet.walletId, any()) } returns mapOf(id to cachedToken)

        // Act
        val result = factory.create(userWallet.walletId, ethereum, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result[id]).isEqualTo(cachedToken)
    }

    @Test
    fun `GIVEN coin asset WHEN create THEN builds coin`() = runTest {
        // Act
        val result = factory.create(
            userWalletId = userWallet.walletId,
            historyCurrency = cachedToken,
            outgoingSwaps = listOf(coinToTokenSwap()),
            incomingSwaps = emptyList(),
            onramps = emptyList(),
        )

        // Assert
        assertThat(result[COIN_ID]).isInstanceOf(CryptoCurrency.Coin::class.java)
    }

    @Test
    fun `GIVEN token absent from portfolio and cache WHEN create THEN omits it`() = runTest {
        // Act
        val result = factory.create(userWallet.walletId, ethereum, listOf(coinToTokenSwap()), emptyList(), emptyList())

        // Assert
        assertThat(result).doesNotContainKey(ExpressAsset.ID(networkId = "ethereum", contractAddress = TOKEN_CONTRACT))
    }

    @Test
    fun `GIVEN joint history currency beside a personal twin WHEN create THEN its asset resolves to the joint one`() =
        runTest {
            // Arrange
            val jointEthereum = ethereum.onJointPath()
            val personalTwin = MockAccounts.createAccount(
                derivationIndex = 1,
                cryptoCurrencies = listOf(ethereum),
                userWalletId = userWallet.walletId,
            )
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListWith(personalTwin)))

            // Act
            val result = factory.create(
                userWalletId = userWallet.walletId,
                historyCurrency = jointEthereum,
                outgoingSwaps = listOf(coinToTokenSwap()),
                incomingSwaps = emptyList(),
                onramps = emptyList(),
            )

            // Assert
            assertThat(result[COIN_ID]).isEqualTo(jointEthereum)
        }

    @Test
    fun `GIVEN personal history currency WHEN create THEN joint twin is never a candidate`() = runTest {
        // Arrange
        val jointTwin = MockAccounts.createJointAccount(
            cryptoCurrencies = listOf(ethereum.onJointPath()),
            userWalletId = userWallet.walletId,
        )
        every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListWith(jointTwin)))

        // Act
        val result = factory.create(
            userWalletId = userWallet.walletId,
            historyCurrency = ethereum,
            outgoingSwaps = listOf(coinToTokenSwap()),
            incomingSwaps = emptyList(),
            onramps = emptyList(),
        )

        // Assert
        assertThat(result[COIN_ID]).isEqualTo(ethereum)
    }

    private fun accountListWith(account: Account): AccountList {
        val mainOnly = MockAccounts.createAccountList(activeAccounts = 1, userWalletId = userWallet.walletId)

        return (mainOnly + account).getOrNull()!!
    }

    private fun CryptoCurrency.Coin.onJointPath(): CryptoCurrency.Coin {
        val jointPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")

        return copy(
            network = network.copy(
                id = network.id.copy(derivationPath = jointPath),
                derivationPath = jointPath,
            ),
        )
    }

    /** A swap from a native coin (empty contract) to an Ethereum token. */
    private fun coinToTokenSwap() = ExpressExchangeEntity(
        txId = "tx-1",
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
        val COIN_ID = ExpressAsset.ID(
            networkId = "ethereum",
            contractAddress = ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE,
        )
    }
}