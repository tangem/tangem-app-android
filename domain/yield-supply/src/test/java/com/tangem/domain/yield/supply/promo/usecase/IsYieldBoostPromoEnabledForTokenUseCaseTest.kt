package com.tangem.domain.yield.supply.promo.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.YieldPromoRepository
import io.mockk.Deregisterable
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.registerInstanceFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IsYieldBoostPromoEnabledForTokenUseCaseTest {

    private val repository: YieldPromoRepository = mockk()
    private lateinit var useCase: IsYieldBoostPromoEnabledForTokenUseCase

    private val userWalletId = UserWalletId("abcdef012345")
    private val contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
    private val networkRawId = "ethereum"

    // Stub concrete instances of the sealed return types so MockK doesn't subclass them while recording coEvery
    // (Objenesis on a JVM-sealed type throws InstantiationError flakily under full-suite CI runs).
    private val instanceFactories = mutableListOf<Deregisterable>()

    @BeforeEach
    fun setUp() {
        instanceFactories += registerInstanceFactory { YieldBoostPromo.None }
        instanceFactories += registerInstanceFactory { YieldBoostStatus.NotStarted }
        useCase = IsYieldBoostPromoEnabledForTokenUseCase(repository = repository)
    }

    @AfterEach
    fun tearDown() {
        instanceFactories.forEach { it.deregister() }
        instanceFactories.clear()
    }

    @Test
    fun `GIVEN currency is coin WHEN invoke THEN returns Right(false)`() = runTest {
        val coin = createCoin()

        val result = useCase(userWalletId, coin)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN promo is None WHEN invoke THEN returns Right(false)`() = runTest {
        val token = createToken()
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns YieldBoostPromo.None

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN promo repository throws WHEN invoke THEN returns Left`() = runTest {
        val token = createToken()
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } throws RuntimeException("net")

        val result = useCase(userWalletId, token)

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN token not in promo list WHEN invoke THEN returns Right(false)`() = runTest {
        val token = createToken(contractAddress = "0xdifferent")
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN network mismatch WHEN invoke THEN returns Right(false)`() = runTest {
        val token = createToken(networkRawId = "polygon")
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN status repository throws WHEN invoke THEN returns Left`() = runTest {
        val token = createToken()
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } throws RuntimeException("net")

        val result = useCase(userWalletId, token)

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN status is Enrolled WHEN invoke THEN returns Right(false)`() = runTest {
        val token = createToken()
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } returns enrolledStatus()

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN status is Disqualified WHEN invoke THEN returns Right(false)`() = runTest {
        val token = createToken()
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } returns
            YieldBoostStatus.Disqualified(YieldBoostStatus.Disqualified.Reason.FROD)

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `GIVEN status is NotStarted and token matches WHEN invoke THEN returns Right(true)`() = runTest {
        val token = createToken()
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } returns YieldBoostStatus.NotStarted

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isTrue()
    }

    @Test
    fun `GIVEN contract address differs only in case on EVM WHEN invoke THEN returns Right(true)`() = runTest {
        val token = createToken(contractAddress = contractAddress.uppercase())
        coEvery { repository.getYieldBoostPromo(userWalletId, false) } returns activePromo()
        coEvery { repository.getYieldBoostStatus(userWalletId, false) } returns YieldBoostStatus.NotStarted

        val result = useCase(userWalletId, token)

        assertThat(result.getOrNull()).isTrue()
    }

    private fun activePromo() = YieldBoostPromo.Active(
        tokens = listOf(
            YieldBoostPromo.Active.PromoToken(
                contractAddress = contractAddress,
                tokenSymbol = "USDC",
                tokenName = "USD Coin",
                networkId = networkRawId,
            ),
        ),
        timeline = YieldBoostPromo.Active.Timeline(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            end = Instant.parse("2027-01-01T00:00:00Z"),
        ),
        link = null,
    )

    private fun enrolledStatus() = YieldBoostStatus.Enrolled(
        tokenName = "USD Coin",
        networkId = networkRawId,
        moduleAddress = "0xmodule",
        userAddress = "0xuser",
        contractAddress = contractAddress,
        qualificationEndDate = Instant.parse("2026-06-01T00:00:00Z"),
    )

    private fun createToken(
        contractAddress: String = this.contractAddress,
        networkRawId: String = this.networkRawId,
    ): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = networkRawId, derivationPath = derivationPath),
            name = networkRawId,
            currencySymbol = networkRawId.take(3).uppercase(),
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
                body = CryptoCurrency.ID.Body.NetworkId(networkRawId),
                suffix = CryptoCurrency.ID.Suffix.RawID(networkRawId),
            ),
            network = network,
            name = "USDC",
            symbol = "USDC",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }

    private fun createCoin(): CryptoCurrency.Coin {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = networkRawId, derivationPath = derivationPath),
            name = networkRawId,
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(networkRawId),
                suffix = CryptoCurrency.ID.Suffix.RawID(networkRawId),
            ),
            network = network,
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )
    }
}