package com.tangem.features.tangempay.deeplink

import android.net.Uri
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()

    private val onboarding = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.Deeplink(deeplink = LINK))

    @BeforeEach
    fun setUp() {
        clearMocks(router, userWalletsListRepository, moneyAccountStatusFetcher, moneyAccountStatusSupplier)
        mockkObject(TangemLogger)
        every { TangemLogger.i(any()) } just Runs
        every { router.popTo(any<AppRoute>(), any()) } answers { secondArg<(Boolean) -> Unit>().invoke(true) }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns wallet(isLocked = false)
        coEvery { moneyAccountStatusFetcher(any<UserWalletId>()) } returns Unit.right()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN open money account WHEN link handled THEN account screen opens on top of the wallet`() = runTest {
        // Arrange
        givenStatus(mockk<MoneyAccountStatusValue.Account.Active>())

        // Act
        handle(scope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 1) {
            router.push(route = AppRoute.MoneyAccountDetails(userWalletId = USER_WALLET_ID), onComplete = any())
        }
    }

    @Test
    fun `GIVEN activation in progress WHEN link handled THEN wallet opens without onboarding`() = runTest {
        // Arrange
        givenStatus(mockk<MoneyAccountStatusValue.Onboarding.Activating>())

        // Act
        handle(scope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no account or unknown state WHEN link handled THEN Tangem Pay onboarding opens`(model: OnboardingModel) =
        runTest {
            // Arrange
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns model.wallet
            if (model.status != null) givenStatus(model.status) else givenNoStatus()

            // Act
            handle(scope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.push(route = onboarding, onComplete = any()) }
        }

    internal data class OnboardingModel(
        val name: String,
        val wallet: UserWallet?,
        val status: MoneyAccountStatusValue?,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        OnboardingModel(name = "no account", wallet = wallet(isLocked = false), status = MoneyAccountStatusValue.Empty),
        OnboardingModel(
            name = "status unavailable",
            wallet = wallet(isLocked = false),
            status = MoneyAccountStatusValue.Error.Unavailable,
        ),
        OnboardingModel(name = "no status emitted", wallet = wallet(isLocked = false), status = null),
        OnboardingModel(name = "no selected wallet", wallet = null, status = null),
        OnboardingModel(name = "locked wallet", wallet = wallet(isLocked = true), status = null),
    )

    private fun givenStatus(value: MoneyAccountStatusValue) {
        val status = mockk<AccountStatus.Money> { every { this@mockk.value } returns value }
        every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(status)
    }

    private fun givenNoStatus() {
        every { moneyAccountStatusSupplier(any<UserWalletId>()) } returns emptyFlow()
    }

    @ParameterizedTest
    @MethodSource("incompleteLinks")
    fun `GIVEN link without entry or id WHEN link handled THEN nothing opens`(queryParams: Map<String, String>) =
        runTest {
            // Act
            handle(scope = this, queryParams = queryParams)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.push(any(), any()) }
            verify(exactly = 0) { router.popTo(any<AppRoute>(), any()) }
            coVerify(exactly = 0) { moneyAccountStatusFetcher(any<UserWalletId>()) }
        }

    private fun incompleteLinks() = listOf(
        mapOf("entry" to "ff1"),
        mapOf("id" to "VRTACC01"),
        mapOf("entry" to "", "id" to "VRTACC01"),
        emptyMap(),
    )

    private fun handle(scope: TestScope, queryParams: Map<String, String> = FULL_PARAMS) = MoneyAccountDeepLinkHandler(
        userWalletsListRepository = userWalletsListRepository,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
        onboardDeepLinkHandler = DefaultTangemPayOnboardDeepLinkHandler(),
    ).handle(
        DeepLinkRequest(
            uri = mockk<Uri> { every { this@mockk.toString() } returns LINK },
            queryParams = queryParams,
            scope = scope,
            isFromOnNewIntent = false,
            router = router,
        ),
    )

    private fun wallet(isLocked: Boolean): UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns USER_WALLET_ID
        every { this@mockk.isLocked } returns isLocked
    }

    private companion object {
        const val LINK = "tangem://onboard-virtual-account?entry=ff1&id=VRTACC01"
        val USER_WALLET_ID = UserWalletId("011")
        val FULL_PARAMS = mapOf("entry" to "ff1", "id" to "VRTACC01")
    }
}