package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.wallets.usecase.DerivationOutcome
import com.tangem.domain.wallets.usecase.DeriveCurrencyAndOwnerKeysUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletWarningsClickIntentsTest {

    private val deriveCurrencyAndOwnerKeysUseCase: DeriveCurrencyAndOwnerKeysUseCase = mockk()
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val currencies = listOf<CryptoCurrency>(mockk(relaxed = true))

    @BeforeEach
    fun setUp() {
        clearMocks(
            deriveCurrencyAndOwnerKeysUseCase,
            multiNetworkStatusFetcher,
            multiQuoteStatusFetcher,
            multiStakingBalanceFetcher,
            stakingIdFactory,
            uiMessageSender,
        )
        coEvery { multiNetworkStatusFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { multiQuoteStatusFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { multiStakingBalanceFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any()) } returns Either.Right(mockk())
    }

    @Test
    fun `GIVEN card derived nothing WHEN generate missed addresses THEN wrong card dialog is shown`() = runTest {
        // Arrange
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns
            Either.Right(DerivationOutcome.NOTHING_DERIVED)
        val message = slot<UiMessage>()

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat((message.captured as DialogMessage).message).isEqualTo(Dialogs.wrongWalletTapped().message)
        coVerify(exactly = 0) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiQuoteStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiStakingBalanceFetcher(params = any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN card derived keys WHEN generate missed addresses THEN currencies are fetched`(
        outcome: DerivationOutcome,
    ) = runTest {
        // Arrange
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns Either.Right(outcome)

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 1) { multiQuoteStatusFetcher(params = any()) }
        coVerify(exactly = 1) { multiStakingBalanceFetcher(params = any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN derivation failed WHEN generate missed addresses THEN nothing is shown or fetched`() = runTest {
        // Arrange
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns
            Either.Left(IllegalStateException("Card session failed"))

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        coVerify(exactly = 0) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiQuoteStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiStakingBalanceFetcher(params = any()) }
    }

    private fun provideTestModels() = listOf(DerivationOutcome.DERIVED, DerivationOutcome.NOTHING_MISSING)

    private fun createImplementor(scope: CoroutineScope): WalletWarningsClickIntentsImplementor {
        val implementor = WalletWarningsClickIntentsImplementor(
            stateHolder = mockk(relaxed = true),
            walletEventSender = mockk(relaxed = true),
            deriveCurrencyAndOwnerKeysUseCase = deriveCurrencyAndOwnerKeysUseCase,
            setCardWasScannedUseCase = mockk(relaxed = true),
            neverToSuggestRateAppUseCase = mockk(relaxed = true),
            remindToRateAppLaterUseCase = mockk(relaxed = true),
            getUserWalletUseCase = mockk(relaxed = true),
            nonBiometricUnlockWalletUseCase = mockk(relaxed = true),
            analyticsEventHandler = mockk(relaxed = true),
            dispatchers = mockk(relaxed = true),
            getWalletMetaInfoUseCase = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            appStoreOpener = mockk(relaxed = true),
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            appRouter = mockk(relaxed = true),
            userWalletsListRepository = mockk(relaxed = true),
            setShouldShowNotificationUseCase = mockk(relaxed = true),
            notificationsRepository = mockk(relaxed = true),
            uiMessageSender = uiMessageSender,
            reviewManager = mockk(relaxed = true),
            closeHotWalletUpgradeBannerUseCase = mockk(relaxed = true),
            acknowledgeAssetsDiscoveryCompletionUseCase = mockk(relaxed = true),
            yieldSupplySetShouldShowMainPromoUseCase = mockk(relaxed = true),
        )
        implementor.initialize(router = mockk(relaxed = true), coroutineScope = scope)

        return implementor
    }
}