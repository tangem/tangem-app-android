package com.tangem.data.collectibles.account.flow

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusProducer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCollectiblesAccountStatusProducerTest {

    private val quoteSupplier: SingleQuoteStatusSupplier = mockk()
    private val walletAccountsFetcher: WalletAccountsFetcher = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val wallets = MutableStateFlow<List<UserWallet>?>(listOf(USER_WALLET))

    @BeforeEach
    fun resetMocks() {
        clearMocks(quoteSupplier, walletAccountsFetcher, ownerDeriver)
        wallets.value = listOf(USER_WALLET)
        arrangeDocument(isListed = false)
        coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER
    }

    @Test
    fun `GIVEN nothing recorded WHEN produce THEN there is no account`() = runTest {
        // Arrange
        arrangeQuotes(rate = null)

        // Act
        val actual = createProducer(createStore(testScope = this)).produce().first()

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountStatusValue.NotOnboarded)
    }

    @Test
    fun `GIVEN a record and no balance read WHEN produce THEN it emits loading at once`() = runTest {
        // Arrange — a quote flow that never emits
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ONBOARDED)
        every {
            quoteSupplier.invoke(SingleQuoteStatusProducer.Params(rawCurrencyId = USDC_RAW_ID))
        } returns MutableSharedFlow()

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountStatusValue.Loading)
    }

    /**
     * The quote producer emits [QuoteStatus.Empty] for any currency nobody has fetched yet, so an empty quote is
     * "no rate so far", not "this has no price". Reporting the balance unpriced would blank the wallet total on
     * every cold start.
     */
    @Test
    fun `GIVEN a balance and an empty quote WHEN produce THEN it keeps reporting loading`() = runTest {
        // Arrange
        arrangeQuotes(rate = null)
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ONBOARDED)
        store.storeBalance(userWalletId = WALLET, balance = BALANCE)

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountStatusValue.Loading)
    }

    @Test
    fun `GIVEN a balance and a quote WHEN produce THEN loading is followed by the priced vault`() = runTest {
        // Arrange
        arrangeQuotes(rate = BigDecimal("150"))
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ONBOARDED)
        store.storeBalance(userWalletId = WALLET, balance = BALANCE)

        // Act & Assert — the rates start pending so the first emission is immediate
        createProducer(store).produce().test {
            assertThat(awaitItem()).isEqualTo(CollectiblesAccountStatusValue.Loading)

            val expected = CollectiblesAccountStatusValue.Onboarded(
                source = StatusSource.ACTUAL,
                vaultBalance = BALANCE.copy(usdcFiatRate = BigDecimal("150")),
            )
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a record whose refresh failed with nothing cached WHEN produce THEN the vault is unavailable`() =
        runTest {
            // Arrange
            arrangeQuotes(rate = BigDecimal("150"))
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ONBOARDED)
            store.markUnrefreshed(userWalletId = WALLET)

            // Act
            val actual = createProducer(store).produce().first()

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountStatusValue.Error.Unavailable)
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the account and the owner key WHEN produce THEN the missed derivation is worked out`(
        model: DerivationModel,
    ) = runTest {
        // Arrange
        arrangeQuotes(rate = null)
        val store = createStore(testScope = this)
        model.recorded?.let { store.store(userWalletId = WALLET, value = it) }
        arrangeDocument(isListed = model.isListed)
        wallets.value = listOf(if (model.isLocked) LOCKED_WALLET else USER_WALLET)
        coEvery { ownerDeriver.storedOwner(WALLET) } returns OWNER.takeUnless { model.isOwnerKeyMissing }

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a missed derivation WHEN the owner key is derived THEN it loads until the vault is read`() = runTest {
        // Arrange
        arrangeQuotes(rate = null)
        val store = createStore(testScope = this)
        arrangeDocument(isListed = true)
        coEvery { ownerDeriver.storedOwner(WALLET) } returnsMany listOf(null, OWNER)

        // Act & Assert
        createProducer(store).produce().test {
            assertThat(awaitItem()).isEqualTo(CollectiblesAccountStatusValue.MissedDerivation)

            wallets.value = listOf(USER_WALLET, mockk<UserWallet.Hot>(relaxed = true))

            assertThat(awaitItem()).isEqualTo(CollectiblesAccountStatusValue.Loading)
        }
    }

    @Test
    fun `GIVEN a backend record with no vault on chain WHEN produce THEN there is no account`() = runTest {
        // Arrange
        arrangeQuotes(rate = null)
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = CollectiblesAccountStatusValue.NotOnboarded)
        arrangeDocument(isListed = true)

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountStatusValue.NotOnboarded)
    }

    private fun provideTestModels() = listOf(
        DerivationModel(isListed = true, expected = CollectiblesAccountStatusValue.MissedDerivation),
        DerivationModel(
            isListed = false,
            recorded = ONBOARDED,
            expected = CollectiblesAccountStatusValue.MissedDerivation,
        ),
        DerivationModel(isListed = false, expected = CollectiblesAccountStatusValue.NotOnboarded),
        DerivationModel(
            isListed = true,
            isOwnerKeyMissing = false,
            expected = CollectiblesAccountStatusValue.Loading,
        ),
        DerivationModel(isListed = true, isLocked = true, expected = CollectiblesAccountStatusValue.Loading),
    )

    data class DerivationModel(
        val isListed: Boolean,
        val expected: CollectiblesAccountStatusValue,
        val recorded: CollectiblesAccountStatusValue? = null,
        val isOwnerKeyMissing: Boolean = true,
        val isLocked: Boolean = false,
    )

    @Test
    fun `GIVEN a stale balance WHEN produce THEN the status is marked as cache-only`() = runTest {
        // Arrange
        arrangeQuotes(rate = BigDecimal("150"))
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = UPDATE_REQUIRED)
        store.storeBalance(userWalletId = WALLET, balance = BALANCE)
        store.markUnrefreshed(userWalletId = WALLET)

        // Act
        val actual = createProducer(store).produce().drop(count = 1).first()

        // Assert
        val expected = CollectiblesAccountStatusValue.UpdateRequired(
            source = StatusSource.ONLY_CACHE,
            vaultBalance = BALANCE.copy(usdcFiatRate = BigDecimal("150")),
        )
        assertThat(actual).isEqualTo(expected)
    }

    /** Stubbed by the exact params: a producer asking for anything but USDC gets no answer and the test fails. */
    private fun arrangeQuotes(rate: BigDecimal?) {
        every {
            quoteSupplier.invoke(SingleQuoteStatusProducer.Params(rawCurrencyId = USDC_RAW_ID))
        } returns quoteFlow(rate)
    }

    private fun quoteFlow(rate: BigDecimal?): Flow<QuoteStatus> {
        val value = if (rate == null) {
            QuoteStatus.Empty
        } else {
            QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = rate,
                fiatRateUSD = rate,
                priceChange = BigDecimal.ZERO,
            )
        }

        return flowOf(QuoteStatus(rawCurrencyId = USDC_RAW_ID, value = value))
    }

    private fun arrangeDocument(isListed: Boolean) {
        val response = mockk<GetWalletAccountsResponse> {
            every { accounts } returns if (isListed) listOf(COLLECTIBLES_RECORD) else emptyList()
        }
        coEvery { walletAccountsFetcher.getSaved(WALLET) } returns response
        every { walletAccountsFetcher.get(WALLET) } returns flowOf(response)
    }

    private fun createStore(testScope: TestScope) = CollectiblesAccountStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = MockStateDataStore(default = emptyMap()),
        converter = CollectiblesAccountStatusValueDTOConverter(
            currencyFactory = CollectiblesVaultCurrencyFactory(MockCollectibles::usdc),
        ),
        scope = TestAppCoroutineScope(testScope),
    )

    private fun createProducer(store: CollectiblesAccountStore) = DefaultCollectiblesAccountStatusProducer(
        params = CollectiblesAccountStatusProducer.Params(userWalletId = WALLET),
        flowProducerTools = mockk(),
        collectiblesAccountStore = store,
        walletAccountsFetcher = walletAccountsFetcher,
        userWalletsListRepository = mockk { every { userWallets } returns wallets },
        collectiblesOwnerDeriver = ownerDeriver,
        singleQuoteStatusSupplier = quoteSupplier,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        const val OWNER = "8kXHqmvJ3sYVsBLnr5aXqjy6r2ZxHhCTvJH1DPDcnUXG"
        val WALLET = UserWalletId("011")

        val USER_WALLET: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns WALLET
            every { isLocked } returns false
        }

        val LOCKED_WALLET: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns WALLET
            every { isLocked } returns true
        }

        val COLLECTIBLES_RECORD = WalletAccountDTO(
            id = "collectibles",
            name = "Collectibles",
            derivationIndex = 999799,
            icon = "Package",
            iconColor = "Azure",
            type = WalletAccountDTO.Type.COLLECTIBLES,
        )
        val USDC_RAW_ID = CollectiblesVaultCurrencies.USDC_RAW_ID

        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE)
        val UPDATE_REQUIRED = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.CACHE)

        val BALANCE = MockCollectibles.vaultBalance(amount = BigDecimal("2"))
    }
}