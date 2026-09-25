package com.tangem.data.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.store.PredictionPositionsValueStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionPositionsValueFetcher
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPredictionPositionsValueFetcherTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val userWallet: UserWallet.Cold = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val positionsRepository: PolymarketPositionsRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository, userWallet, deriveAddresses, positionsRepository)
        every { userWallet.walletId } returns WALLET
        every { userWallet.isLocked } returns false
        every { userWalletsListRepository.userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(userWallet))
        coEvery { deriveAddresses.stored(WALLET) } returns ADDRESSES
    }

    @Test
    fun `GIVEN the value is read WHEN invoke THEN it is stored as actual`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("7.5").right()
        val store = PredictionPositionsValueStore()

        // Act
        val actual = createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(actual.isRight()).isTrue()
        assertThat(store.get(WALLET).first())
            .isEqualTo(PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ACTUAL))
    }

    @Test
    fun `GIVEN no stored addresses WHEN invoke THEN nothing is read or stored`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(WALLET) } returns null
        val store = PredictionPositionsValueStore()

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isNull()
        coVerify(exactly = 0) { positionsRepository.getPositionsValue(any()) }
    }

    @Test
    fun `GIVEN a locked wallet with a cached value WHEN invoke THEN nothing is read and the value is flagged`() =
        runTest {
            // Arrange
            coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("7.5").right()
            val store = PredictionPositionsValueStore()
            val fetcher = createFetcher(store)
            fetcher.invoke(PARAMS)
            every { userWallet.isLocked } returns true

            // Act
            fetcher.invoke(PARAMS)

            // Assert
            assertThat(store.get(WALLET).first())
                .isEqualTo(PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ONLY_CACHE))
            coVerify(exactly = 1) { positionsRepository.getPositionsValue(any()) }
        }

    @Test
    fun `GIVEN a wallet that is not in the list WHEN invoke THEN nothing is read or stored`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow<List<UserWallet>?>(emptyList())
        val store = PredictionPositionsValueStore()

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isNull()
        coVerify(exactly = 0) { positionsRepository.getPositionsValue(any()) }
    }

    @Test
    fun `GIVEN a cached value WHEN the read fails THEN the cached value is kept and flagged`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("7.5").right() andThen
            PolymarketPositionsError.Network.left()
        val store = PredictionPositionsValueStore()
        val fetcher = createFetcher(store)
        fetcher.invoke(PARAMS)

        // Act
        fetcher.invoke(PARAMS)

        // Assert
        assertThat(store.get(WALLET).first())
            .isEqualTo(PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN nothing cached WHEN the read fails THEN nothing is stored`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns PolymarketPositionsError.Network.left()
        val store = PredictionPositionsValueStore()

        // Act
        createFetcher(store).invoke(PARAMS)

        // Assert
        assertThat(store.getSyncOrNull(WALLET)).isNull()
    }

    @Test
    fun `GIVEN a cached value WHEN the addresses cannot be read THEN the cached value is flagged`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPositionsValue(ADDRESSES) } returns BigDecimal("7.5").right()
        val store = PredictionPositionsValueStore()
        val fetcher = createFetcher(store)
        fetcher.invoke(PARAMS)
        coEvery { deriveAddresses.stored(WALLET) } throws IllegalStateException("boom")

        // Act
        val actual = fetcher.invoke(PARAMS)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(store.getSyncOrNull(WALLET)?.source).isEqualTo(StatusSource.ONLY_CACHE)
    }

    private fun createFetcher(store: PredictionPositionsValueStore) = DefaultPredictionPositionsValueFetcher(
        store = store,
        userWalletsListRepository = userWalletsListRepository,
        derivePolymarketAddressesUseCase = deriveAddresses,
        positionsRepository = positionsRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val WALLET = UserWalletId("011")
        val PARAMS = PredictionPositionsValueFetcher.Params(userWalletId = WALLET)
        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true)
    }
}