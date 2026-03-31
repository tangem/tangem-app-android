package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Approver
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class DefaultAllowanceRepositoryTest {

    private val userWalletId = UserWalletId(stringValue = "1234567890ABCDEF")
    private val spenderAddress = "0xSpender"

    private val approverWalletManager: WalletManager =
        mockk<WalletManager>(moreInterfaces = arrayOf(Approver::class))

    private val walletManagersFacade: WalletManagersFacade = mockk {
        coEvery { getOrCreateWalletManager(userWalletId, any<Network>()) } returns approverWalletManager
    }

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private lateinit var repository: DefaultAllowanceRepository

    @BeforeEach
    fun setup() {
        repository = DefaultAllowanceRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }

    // region getAllowance

    @Nested
    inner class GetAllowanceTests {

        @Test
        fun `throws when cryptoCurrency is Coin`() = runTest {
            val coin = buildCoin()

            assertThrows<IllegalStateException> {
                repository.getAllowance(userWalletId, coin, spenderAddress)
            }
        }

        @Test
        fun `throws when walletManager is null`() = runTest {
            val token = buildToken()
            coEvery {
                walletManagersFacade.getOrCreateWalletManager(userWalletId, token.network)
            } returns null

            assertThrows<IllegalStateException> {
                repository.getAllowance(userWalletId, token, spenderAddress)
            }
        }

        @Test
        fun `throws when walletManager is not Approver`() = runTest {
            val token = buildToken()
            val nonApproverWalletManager: WalletManager = mockk()
            coEvery {
                walletManagersFacade.getOrCreateWalletManager(userWalletId, token.network)
            } returns nonApproverWalletManager

            assertThrows<IllegalStateException> {
                repository.getAllowance(userWalletId, token, spenderAddress)
            }
        }

        @Test
        fun `returns allowance on success`() = runTest {
            val token = buildToken()
            val expected = BigDecimal("100")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any<Token>())
            } returns Result.success(expected)

            val result = repository.getAllowance(userWalletId, token, spenderAddress)

            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `throws when approver returns failure`() = runTest {
            val token = buildToken()
            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any<Token>())
            } returns Result.failure(RuntimeException("rpc error"))

            assertThrows<IllegalStateException> {
                repository.getAllowance(userWalletId, token, spenderAddress)
            }
        }
    }

    // endregion

    // region getAllowanceInfo

    @Nested
    inner class GetAllowanceInfoTests {

        @Test
        fun `throws when cryptoCurrency is Coin`() = runTest {
            val coin = buildCoin()

            assertThrows<IllegalStateException> {
                repository.getAllowanceInfo(userWalletId, coin, spenderAddress, BigDecimal.ONE)
            }
        }

        @Test
        fun `returns Enough when allowance equals required amount`() = runTest {
            val token = buildToken(rawNetworkId = "polygon", rawCurrencyId = "usd-coin")
            val amount = BigDecimal("100")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(amount)

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, amount)

            assertThat(result).isInstanceOf(AllowanceInfo.Enough::class.java)
            assertThat((result as AllowanceInfo.Enough).allowance).isEqualTo(amount)
        }

        @Test
        fun `returns Enough when allowance exceeds required amount`() = runTest {
            val token = buildToken(rawNetworkId = "polygon", rawCurrencyId = "usd-coin")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal("200"))

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("100"))

            assertThat(result).isInstanceOf(AllowanceInfo.Enough::class.java)
            assertThat((result as AllowanceInfo.Enough).allowance).isEqualTo(BigDecimal("200"))
        }

        @Test
        fun `returns NotEnough when allowance is zero`() = runTest {
            val token = buildToken(rawNetworkId = "ethereum", rawCurrencyId = "tether")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal.ZERO)

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("50"))

            assertThat(result).isInstanceOf(AllowanceInfo.NotEnough::class.java)
            result as AllowanceInfo.NotEnough
            assertThat(result.allowance).isEqualTo(BigDecimal.ZERO)
            assertThat(result.requiredAmount).isEqualTo(BigDecimal("50"))
        }

        @Test
        fun `returns NotEnough when partial allowance for non-tether token`() = runTest {
            val token = buildToken(rawNetworkId = "ethereum", rawCurrencyId = "usd-coin")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal("30"))

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("100"))

            assertThat(result).isInstanceOf(AllowanceInfo.NotEnough::class.java)
            result as AllowanceInfo.NotEnough
            assertThat(result.allowance).isEqualTo(BigDecimal("30"))
            assertThat(result.requiredAmount).isEqualTo(BigDecimal("100"))
        }

        @Test
        fun `returns NotEnough when partial allowance for tether on non-ethereum network`() = runTest {
            val token = buildToken(rawNetworkId = "polygon", rawCurrencyId = "tether")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal("30"))

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("100"))

            assertThat(result).isInstanceOf(AllowanceInfo.NotEnough::class.java)
        }

        @Test
        fun `returns ResetNeeded when partial allowance for tether on ethereum`() = runTest {
            val token = buildToken(rawNetworkId = "ETH", rawCurrencyId = "tether")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal("30"))

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("100"))

            assertThat(result).isInstanceOf(AllowanceInfo.ResetNeeded::class.java)
            result as AllowanceInfo.ResetNeeded
            assertThat(result.allowance).isEqualTo(BigDecimal("30"))
            assertThat(result.requiredAmount).isEqualTo(BigDecimal("100"))
        }

        @Test
        fun `returns ResetNeeded when partial allowance for tether on ethereum testnet`() = runTest {
            val token = buildToken(rawNetworkId = "ETH/test", rawCurrencyId = "tether")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal("10"))

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("50"))

            assertThat(result).isInstanceOf(AllowanceInfo.ResetNeeded::class.java)
        }

        @Test
        fun `returns Enough for tether on ethereum when allowance is sufficient`() = runTest {
            val token = buildToken(rawNetworkId = "ethereum", rawCurrencyId = "tether")

            coEvery {
                (approverWalletManager as Approver).getAllowance(spenderAddress, any())
            } returns Result.success(BigDecimal("100"))

            val result = repository.getAllowanceInfo(userWalletId, token, spenderAddress, BigDecimal("100"))

            assertThat(result).isInstanceOf(AllowanceInfo.Enough::class.java)
        }
    }

    // endregion

    // region Helpers

    private fun buildNetwork(rawNetworkId: String): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(Network.RawID(rawNetworkId), derivationPath),
            backendId = rawNetworkId,
            name = rawNetworkId.replaceFirstChar { it.uppercase() },
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = rawNetworkId.contains("test"),
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private fun buildToken(
        rawNetworkId: String = "ETH",
        rawCurrencyId: String = "tether",
        contractAddress: String = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
    ): CryptoCurrency.Token {
        val network = buildNetwork(rawNetworkId)
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawCurrencyId),
            ),
            network = network,
            name = "Token",
            symbol = "TKN",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }

    private fun buildCoin(rawNetworkId: String = "ethereum"): CryptoCurrency.Coin {
        val network = buildNetwork(rawNetworkId)
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
            ),
            network = network,
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )
    }

    // endregion
}
