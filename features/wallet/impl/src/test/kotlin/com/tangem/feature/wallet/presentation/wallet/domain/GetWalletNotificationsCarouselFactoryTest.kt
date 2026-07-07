package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.yield.supply.promo.usecase.ShouldShowYieldBoostMainBannerUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetWalletNotificationsCarouselFactoryTest {

    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val notificationsRepository: NotificationsRepository = mockk()
    private val shouldShowYieldBoostMainBannerUseCase: ShouldShowYieldBoostMainBannerUseCase = mockk()
    private val yieldSupplyGetShouldShowMainPromoUseCase: YieldSupplyGetShouldShowMainPromoUseCase = mockk()
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val clickIntents: WalletClickIntents = mockk(relaxed = true)
    private val userWallet: UserWallet.Hot = mockk(relaxed = true)

    private val factory = GetWalletNotificationsCarouselFactory(
        isReadyToShowRateAppUseCase = isReadyToShowRateAppUseCase,
        getWalletsUseCase = getWalletsUseCase,
        notificationsRepository = notificationsRepository,
        shouldShowYieldBoostMainBannerUseCase = shouldShowYieldBoostMainBannerUseCase,
        yieldSupplyGetShouldShowMainPromoUseCase = yieldSupplyGetShouldShowMainPromoUseCase,
        yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            isReadyToShowRateAppUseCase,
            getWalletsUseCase,
            notificationsRepository,
            shouldShowYieldBoostMainBannerUseCase,
            yieldSupplyGetShouldShowMainPromoUseCase,
            yieldSupplyFeatureToggles,
            singleAccountStatusListSupplier,
            clickIntents,
            userWallet,
        )
        // Defaults: only the yield boost banner can appear; every other notification is suppressed.
        every { userWallet.walletId } returns WALLET_ID
        every { notificationsRepository.getShouldShowNotification(any()) } returns flowOf(false)
        coEvery { notificationsRepository.isUserAllowToSubscribeOnPushNotifications() } returns true
        every { isReadyToShowRateAppUseCase() } returns flowOf(false)
        every { getWalletsUseCase() } returns flowOf(emptyList())
        every { yieldSupplyGetShouldShowMainPromoUseCase() } returns flowOf(true)
        every { yieldSupplyFeatureToggles.isYieldPromoEnabled } returns true
        coEvery { shouldShowYieldBoostMainBannerUseCase(any()) } returns Either.Right(true)
        // Balance is loaded by default, so banners gated on balance are not suppressed.
        every {
            singleAccountStatusListSupplier(any<SingleAccountStatusListProducer.Params>())
        } returns flowOf(accountStatusList(TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL)))
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ScanResponse::cardTypesResolver)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN gating conditions WHEN create THEN yield boost banner visibility matches`(model: Model) = runTest {
        // Arrange
        every { yieldSupplyFeatureToggles.isYieldPromoEnabled } returns model.toggleEnabled
        every { yieldSupplyGetShouldShowMainPromoUseCase() } returns flowOf(model.shouldShowLocal)
        coEvery { shouldShowYieldBoostMainBannerUseCase(WALLET_ID) } returns model.mainBanner

        // Act
        val result = factory.create(userWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.YieldBoostPromo }).isEqualTo(model.expectedShown)
    }

    @ParameterizedTest
    @MethodSource("provideRateAppTestModels")
    fun `GIVEN ready to show rate app and balance state WHEN create THEN rate app banner visibility matches`(
        model: RateAppModel,
    ) = runTest {
        // Arrange
        every { isReadyToShowRateAppUseCase() } returns flowOf(model.isReadyToShow)
        every {
            singleAccountStatusListSupplier(any<SingleAccountStatusListProducer.Params>())
        } returns flowOf(accountStatusList(model.balance))

        // Act
        val result = factory.create(userWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.RateApp }).isEqualTo(model.expectedShown)
    }

    @Test
    fun `GIVEN banner shown WHEN buttons clicked THEN routes to click intents`() = runTest {
        // Arrange
        val banner = factory.create(userWallet, clickIntents).first()
            .filterIsInstance<WalletNotificationUM.YieldBoostPromo>()
            .first()

        // Act
        banner.onExploreClick()
        banner.onLaterClick()

        // Assert
        verify { clickIntents.onYieldBoostBannerClick(WALLET_ID) }
        verify { clickIntents.onDismissYieldBoostBanner(WALLET_ID) }
    }

    @ParameterizedTest
    @MethodSource("provideNoteMigrationTestModels")
    fun `GIVEN single-currency card WHEN create THEN discover wallet promo visibility matches`(
        model: NoteMigrationModel,
    ) = runTest {
        // Arrange
        mockkStatic(ScanResponse::cardTypesResolver)
        val selectedResolver = mockk<CardTypesResolver>(relaxed = true) {
            every { isSingleCurrency() } returns model.isSingleCurrency
        }
        val selectedWallet = mockColdWallet(selectedResolver, walletId = WALLET_ID)
        val wallets = buildList {
            add(selectedWallet)
            if (model.userAlreadyHasWallet) {
                val walletResolver = mockk<CardTypesResolver>(relaxed = true) {
                    every { isTangemWallet() } returns true
                }
                add(mockColdWallet(walletResolver, walletId = OTHER_WALLET_ID))
            }
        }
        every { getWalletsUseCase() } returns flowOf(wallets)

        // Act
        val result = factory.create(selectedWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.NoteMigration }).isEqualTo(model.expectedShown)
    }

    @Test
    fun `GIVEN hot wallet WHEN create THEN discover wallet promo is hidden`() = runTest {
        // Arrange
        every { getWalletsUseCase() } returns flowOf(listOf(userWallet))

        // Act
        val result = factory.create(userWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.NoteMigration }).isTrue()
    }

    private fun mockColdWallet(resolver: CardTypesResolver, walletId: UserWalletId): UserWallet.Cold {
        val scanResponse = mockk<ScanResponse>()
        every { scanResponse.cardTypesResolver } returns resolver
        return mockk<UserWallet.Cold>(relaxed = true) {
            every { this@mockk.walletId } returns walletId
            every { this@mockk.scanResponse } returns scanResponse
        }
    }

    private fun accountStatusList(balance: TotalFiatBalance) = AccountStatusList(
        userWalletId = WALLET_ID,
        accountStatuses = emptyList(),
        totalAccounts = 0,
        totalArchivedAccounts = 0,
        totalFiatBalance = balance,
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    internal data class Model(
        val toggleEnabled: Boolean,
        val shouldShowLocal: Boolean,
        val mainBanner: Either<Throwable, Boolean>,
        val expectedShown: Boolean,
    )

    private fun provideTestModels() = listOf(
        Model(toggleEnabled = true, shouldShowLocal = true, mainBanner = Either.Right(true), expectedShown = true),
        Model(toggleEnabled = false, shouldShowLocal = true, mainBanner = Either.Right(true), expectedShown = false),
        Model(toggleEnabled = true, shouldShowLocal = false, mainBanner = Either.Right(true), expectedShown = false),
        Model(toggleEnabled = true, shouldShowLocal = true, mainBanner = Either.Right(false), expectedShown = false),
        Model(
            toggleEnabled = true,
            shouldShowLocal = true,
            mainBanner = Either.Left(RuntimeException("boom")),
            expectedShown = false,
        ),
    )

    internal data class RateAppModel(
        val isReadyToShow: Boolean,
        val balance: TotalFiatBalance,
        val expectedShown: Boolean,
    )

    private fun provideRateAppTestModels() = listOf(
        // Ready to show, but the balance is still loading — don't flash before Add Funds may appear.
        RateAppModel(isReadyToShow = true, balance = TotalFiatBalance.Loading, expectedShown = false),
        // Ready to show and the balance is loaded — the banner can appear.
        RateAppModel(
            isReadyToShow = true,
            balance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            expectedShown = true,
        ),
        // Ready to show and the balance failed — terminal state, only loading suppresses the banner.
        RateAppModel(isReadyToShow = true, balance = TotalFiatBalance.Failed, expectedShown = true),
        // Not ready to show — the banner stays hidden regardless of the balance state.
        RateAppModel(
            isReadyToShow = false,
            balance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            expectedShown = false,
        ),
    )

    internal data class NoteMigrationModel(
        val isSingleCurrency: Boolean,
        val userAlreadyHasWallet: Boolean,
        val expectedShown: Boolean,
    )

    private fun provideNoteMigrationTestModels() = listOf(
        // Single-currency card (Note / S2C / Twins) and the user owns no multi-currency wallet — promo shown.
        NoteMigrationModel(isSingleCurrency = true, userAlreadyHasWallet = false, expectedShown = true),
        // Single-currency card, but the user already owns a Wallet / Wallet2 — promo hidden.
        NoteMigrationModel(isSingleCurrency = true, userAlreadyHasWallet = true, expectedShown = false),
        // Multi-currency card — promo hidden.
        NoteMigrationModel(isSingleCurrency = false, userAlreadyHasWallet = false, expectedShown = false),
    )

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val OTHER_WALLET_ID = UserWalletId("02")
    }
}