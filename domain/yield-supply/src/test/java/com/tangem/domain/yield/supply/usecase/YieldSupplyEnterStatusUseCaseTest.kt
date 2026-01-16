package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class YieldSupplyEnterStatusUseCaseTest {

    private val yieldSupplyRepository: YieldSupplyRepository = mockk()

    private lateinit var useCase: YieldSupplyEnterStatusUseCase

    private val userWalletId = UserWalletId("abcdef012345")

    @BeforeEach
    fun setUp() {
        useCase = YieldSupplyEnterStatusUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Test
    fun `GIVEN pending tx exists in status WHEN invoke THEN returns status`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)
        val pendingTxHash = "0xabc123"
        val status = YieldSupplyPendingStatus.Enter(txIds = listOf(pendingTxHash))

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } returns status
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoStatus)
        } returns listOf(pendingTxHash)

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val value = (result as Either.Right).value
        assertThat(value).isEqualTo(status)
    }

    @Test
    fun `GIVEN no pending tx matches status WHEN invoke THEN clears status and returns null`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)
        val status = YieldSupplyPendingStatus.Enter(txIds = listOf("0xabc123"))

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } returns status
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoStatus)
        } returns listOf("0xdifferent")
        coEvery {
            yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, token, null)
        } returns Unit

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val value = (result as Either.Right).value
        assertThat(value).isNull()
        coVerify { yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, token, null) }
    }

    @Test
    fun `GIVEN status is null WHEN invoke THEN returns null`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } returns null
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoStatus)
        } returns emptyList()
        coEvery {
            yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, token, null)
        } returns Unit

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val value = (result as Either.Right).value
        assertThat(value).isNull()
    }

    @Test
    fun `GIVEN empty pending tx list WHEN invoke THEN clears status and returns null`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)
        val status = YieldSupplyPendingStatus.Enter(txIds = listOf("0xabc123"))

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } returns status
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoStatus)
        } returns emptyList()
        coEvery {
            yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, token, null)
        } returns Unit

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val value = (result as Either.Right).value
        assertThat(value).isNull()
        coVerify { yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, token, null) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke THEN returns left with error`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)
        val exception = RuntimeException("Network error")

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } throws exception

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isLeft()).isTrue()
        val error = (result as Either.Left).value
        assertThat(error).isEqualTo(exception)
    }

    @Test
    fun `GIVEN exit status with pending tx WHEN invoke THEN returns status`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)
        val pendingTxHash = "0xexit456"
        val status = YieldSupplyPendingStatus.Exit(txIds = listOf(pendingTxHash))

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } returns status
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoStatus)
        } returns listOf(pendingTxHash)

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val value = (result as Either.Right).value
        assertThat(value).isEqualTo(status)
    }

    @Test
    fun `GIVEN multiple tx ids with one matching WHEN invoke THEN returns status`() = runTest {
        val token = createToken()
        val cryptoStatus = createStatus(token)
        val matchingTxHash = "0xmatch"
        val status = YieldSupplyPendingStatus.Enter(txIds = listOf("0xfirst", matchingTxHash, "0xlast"))

        coEvery {
            yieldSupplyRepository.getTokenProtocolPendingStatus(userWalletId, token)
        } returns status
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, cryptoStatus)
        } returns listOf(matchingTxHash)

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val value = (result as Either.Right).value
        assertThat(value).isEqualTo(status)
    }

    private fun createToken(): CryptoCurrency.Token {
        val rawNetworkId = "ethereum"
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = rawNetworkId, derivationPath = derivationPath),
            backendId = rawNetworkId,
            name = rawNetworkId,
            currencySymbol = rawNetworkId.take(3).uppercase(),
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = network,
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xToken",
        )
    }

    private fun createStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ZERO,
                fiatAmount = BigDecimal.ZERO,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = "0x0000000000000000000000000000000000000000",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }
}