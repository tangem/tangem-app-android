package com.tangem.data.txhistory.repository.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.local.txhistory.db.dao.TokenInfoDao
import com.tangem.datasource.local.txhistory.db.entity.express.TokenInfoEntity
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenInfoRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val tokenInfoDao: TokenInfoDao = mockk(relaxUnitFun = true)
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()

    private val repository = TokenInfoRepository(
        tangemTechApi = tangemTechApi,
        tokenInfoDao = tokenInfoDao,
        multiAccountListSupplier = multiAccountListSupplier,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(tangemTechApi, tokenInfoDao, multiAccountListSupplier)
        every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListOf()))
    }

    @Test
    fun `GIVEN only coins WHEN fetchMissing THEN nothing is read or fetched`() = runTest {
        // Act
        repository.fetchMissing(setOf(ExpressAsset.ID(networkId = "ethereum", contractAddress = COIN_CONTRACT)))

        // Assert
        coVerify(exactly = 0) { tokenInfoDao.getCached(any(), any(), any()) }
        coVerify(exactly = 0) { tangemTechApi.getCoins(networkIds = any(), contractAddresses = any(), active = any()) }
        coVerify(exactly = 0) { tokenInfoDao.upsert(any()) }
    }

    @Test
    fun `GIVEN token already fresh in cache WHEN fetchMissing THEN does not fetch`() = runTest {
        // Arrange
        coEvery { tokenInfoDao.getCached(any(), any(), any()) } returns listOf(
            tokenInfoEntity(networkId = "ethereum", contractAddress = "0xUSDT"),
        )

        // Act
        repository.fetchMissing(setOf(ExpressAsset.ID(networkId = "ethereum", contractAddress = "0xUSDT")))

        // Assert
        coVerify(exactly = 0) { tangemTechApi.getCoins(networkIds = any(), contractAddresses = any(), active = any()) }
        coVerify(exactly = 0) { tokenInfoDao.upsert(any()) }
    }

    @Test
    fun `GIVEN token already in portfolio WHEN fetchMissing THEN does not fetch`() = runTest {
        // Arrange
        coEvery { tokenInfoDao.getCached(any(), any(), any()) } returns emptyList()
        val portfolioToken = MockCryptoCurrencyFactory().createToken(
            blockchain = Blockchain.Ethereum,
            contractAddress = "0xPortfolioToken",
        )
        every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListOf(portfolioToken)))

        // Act — same contract, different casing
        repository.fetchMissing(setOf(ExpressAsset.ID(networkId = "ethereum", contractAddress = "0XPORTFOLIOTOKEN")))

        // Assert
        coVerify(exactly = 0) { tangemTechApi.getCoins(networkIds = any(), contractAddresses = any(), active = any()) }
        coVerify(exactly = 0) { tokenInfoDao.upsert(any()) }
    }

    @Test
    fun `GIVEN unresolved token WHEN fetchMissing THEN fetches and caches it`() = runTest {
        // Arrange
        coEvery { tokenInfoDao.getCached(any(), any(), any()) } returns emptyList()
        coEvery {
            tangemTechApi.getCoins(networkIds = any(), contractAddresses = any(), active = any())
        } returns ApiResponse.Success(
            coinsResponse(coin(network("ethereum", "0xUSDT", BigDecimal(6)))),
        )
        val saved = slot<List<TokenInfoEntity>>()
        coEvery { tokenInfoDao.upsert(capture(saved)) } returns Unit

        // Act
        repository.fetchMissing(setOf(ExpressAsset.ID(networkId = "ethereum", contractAddress = "0xUSDT")))

        // Assert
        val entity = saved.captured.single()
        assertThat(entity.copy(updatedAt = 0)).isEqualTo(
            TokenInfoEntity(
                networkId = "ethereum",
                contractAddress = "0xUSDT",
                coinId = COIN_ID,
                name = COIN_NAME,
                symbol = COIN_SYMBOL,
                decimals = 6,
                updatedAt = 0,
            ),
        )
    }

    @Test
    fun `GIVEN response with extra networks WHEN fetchMissing THEN keeps only requested pairs ignoring case`() = runTest {
        // Arrange
        coEvery { tokenInfoDao.getCached(any(), any(), any()) } returns emptyList()
        coEvery {
            tangemTechApi.getCoins(networkIds = any(), contractAddresses = any(), active = any())
        } returns ApiResponse.Success(
            coinsResponse(
                coin(
                    network("ethereum", "0xabc", BigDecimal(6)), // requested (different case)
                    network("bsc", "0xabc", BigDecimal(18)), // other network — not requested
                    network("ethereum", "0xother", null), // missing decimals — dropped
                ),
            ),
        )
        val saved = slot<List<TokenInfoEntity>>()
        coEvery { tokenInfoDao.upsert(capture(saved)) } returns Unit

        // Act
        repository.fetchMissing(setOf(ExpressAsset.ID(networkId = "ethereum", contractAddress = "0xAbC")))

        // Assert
        assertThat(saved.captured.map { it.networkId to it.contractAddress })
            .containsExactly("ethereum" to "0xabc")
    }

    @Test
    fun `GIVEN getCoins fails WHEN fetchMissing THEN nothing is cached`() = runTest {
        // Arrange
        coEvery { tokenInfoDao.getCached(any(), any(), any()) } returns emptyList()
        coEvery {
            tangemTechApi.getCoins(networkIds = any(), contractAddresses = any(), active = any())
        } returns ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
                message = "boom",
                errorBody = null,
            ),
        ).cast()

        // Act
        repository.fetchMissing(setOf(ExpressAsset.ID(networkId = "ethereum", contractAddress = "0xUSDT")))

        // Assert
        coVerify(exactly = 0) { tokenInfoDao.upsert(any()) }
    }

    private fun accountListOf(vararg currencies: CryptoCurrency): AccountList = mockk {
        every { flattenCurrencies() } returns currencies.toList()
    }

    private fun tokenInfoEntity(networkId: String, contractAddress: String) = TokenInfoEntity(
        networkId = networkId,
        contractAddress = contractAddress,
        coinId = COIN_ID,
        name = COIN_NAME,
        symbol = COIN_SYMBOL,
        decimals = 6,
        updatedAt = 0,
    )

    private fun coinsResponse(vararg coins: CoinsResponse.Coin) = CoinsResponse(
        imageHost = null,
        coins = coins.toList(),
        total = coins.size,
    )

    private fun coin(vararg networks: CoinsResponse.Coin.Network) = CoinsResponse.Coin(
        id = COIN_ID,
        name = COIN_NAME,
        symbol = COIN_SYMBOL,
        active = true,
        networks = networks.toList(),
    )

    private fun network(networkId: String, contractAddress: String?, decimalCount: BigDecimal?) =
        CoinsResponse.Coin.Network(
            networkId = networkId,
            contractAddress = contractAddress,
            decimalCount = decimalCount,
            exchangeable = true,
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ApiResponse.Error.cast(): ApiResponse<T> = this as ApiResponse<T>

    private companion object {
        const val COIN_CONTRACT = "0"
        const val COIN_ID = "tether"
        const val COIN_NAME = "Tether"
        const val COIN_SYMBOL = "USDT"
    }
}