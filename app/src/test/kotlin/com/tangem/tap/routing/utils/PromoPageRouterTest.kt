package com.tangem.tap.routing.utils

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.uri.PromoPageUrlValidator
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.PromoPageRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoPageRouterTest {

    private val keptPage = MutableStateFlow<String?>(null)
    private val selectedWallet = MutableStateFlow<UserWallet?>(null)
    private val promoPageRepository: PromoPageRepository = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val marketingFeatureToggles: MarketingFeatureToggles = mockk()
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val unlockedWallet: UserWallet.Hot = mockk { every { isLocked } returns false }
    private val lockedWallet: UserWallet.Hot = mockk { every { isLocked } returns true }

    private val router = PromoPageRouter(
        promoPageRepository = promoPageRepository,
        userWalletsListRepository = userWalletsListRepository,
        marketingFeatureToggles = marketingFeatureToggles,
        appRouter = appRouter,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeAll
    fun mockUrlValidation() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        mockkObject(PromoPageUrlValidator)
    }

    @AfterAll
    fun unmockUrlValidation() {
        unmockkObject(PromoPageUrlValidator)
        unmockkStatic(Uri::class)
    }

    @BeforeEach
    fun setUp() {
        clearMocks(promoPageRepository, userWalletsListRepository, marketingFeatureToggles, appRouter)
        keptPage.value = null
        selectedWallet.value = unlockedWallet
        every { promoPageRepository.observePage() } returns keptPage
        coEvery { promoPageRepository.setPage(any()) } answers { keptPage.value = firstArg() }
        coEvery { promoPageRepository.consumePage() } answers { keptPage.getAndUpdate { null } }
        every { userWalletsListRepository.selectedUserWallet } returns selectedWallet
        every { marketingFeatureToggles.isPromoPageEnabled } returns true
        every { PromoPageUrlValidator.isAllowed(any()) } returns true
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SavePage {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN deep link WHEN savePage THEN only a promo_page link replaces the kept page`(model: SavePageModel) {
            // Arrange
            keptPage.value = OLD_PAGE
            every { marketingFeatureToggles.isPromoPageEnabled } returns model.isToggleOn
            val uri = deepLinkUri(scheme = model.scheme, host = model.host, pages = model.pages)

            // Act
            router.savePage(uri)

            // Assert
            assertThat(keptPage.value).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SavePageModel(name = "first non-empty page", pages = listOf("", PAGE, OLD_PAGE), expected = PAGE),
            SavePageModel(name = "no page", pages = emptyList(), expected = null),
            SavePageModel(name = "empty page", pages = listOf(""), expected = null),
            SavePageModel(name = "toggle off", isToggleOn = false, expected = OLD_PAGE),
            SavePageModel(name = "another target", host = "main", expected = OLD_PAGE),
            SavePageModel(name = "web link", scheme = "https", expected = OLD_PAGE),
            SavePageModel(name = "opaque wc link", scheme = "wc", host = null, expected = OLD_PAGE),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Observe {

        @Test
        fun `GIVEN kept page WHEN main screen shown twice THEN the page opens once`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                keptPage.value = PAGE
                val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.AppSettings)
                router.observe(backgroundScope, currentRoute)

                // Act
                currentRoute.value = AppRoute.Wallet
                currentRoute.value = AppRoute.PromoPage(PAGE)
                currentRoute.value = AppRoute.Wallet

                // Assert
                verify(exactly = 1) { appRouter.push(AppRoute.PromoPage(PAGE), any()) }
                assertThat(keptPage.value).isNull()
            }

        @Test
        fun `GIVEN main screen shown WHEN page kept THEN the page opens`() = runTest(UnconfinedTestDispatcher()) {
            // Arrange
            router.observe(backgroundScope, MutableStateFlow(AppRoute.Wallet))

            // Act
            keptPage.value = PAGE

            // Assert
            verify(exactly = 1) { appRouter.push(AppRoute.PromoPage(PAGE), any()) }
        }

        @Test
        fun `GIVEN locked wallet on main screen WHEN unlocked THEN the page opens`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                keptPage.value = PAGE
                selectedWallet.value = lockedWallet
                router.observe(backgroundScope, MutableStateFlow(AppRoute.Wallet))

                // Act
                selectedWallet.value = unlockedWallet

                // Assert
                verify(exactly = 1) { appRouter.push(AppRoute.PromoPage(PAGE), any()) }
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN kept page WHEN it can not be shown yet THEN the page waits`(model: WaitingPageModel) =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                keptPage.value = PAGE
                selectedWallet.value = model.selectedWallet
                every { marketingFeatureToggles.isPromoPageEnabled } returns model.isToggleOn

                // Act
                router.observe(backgroundScope, MutableStateFlow(model.route))

                // Assert
                verify(exactly = 0) { appRouter.push(any(), any()) }
                assertThat(keptPage.value).isEqualTo(PAGE)
            }

        @Test
        fun `GIVEN not allowed page WHEN main screen shown THEN the page is dropped unopened`() =
            runTest(UnconfinedTestDispatcher()) {
                // Arrange
                keptPage.value = PAGE
                every { PromoPageUrlValidator.isAllowed(any()) } returns false

                // Act
                router.observe(backgroundScope, MutableStateFlow(AppRoute.Wallet))

                // Assert
                verify(exactly = 0) { appRouter.push(any(), any()) }
                assertThat(keptPage.value).isNull()
            }

        private fun provideTestModels() = listOf(
            WaitingPageModel(name = "another screen", selectedWallet = unlockedWallet, route = AppRoute.AppSettings),
            WaitingPageModel(name = "locked wallet", selectedWallet = lockedWallet),
            WaitingPageModel(name = "no selected wallet", selectedWallet = null),
            WaitingPageModel(name = "toggle off", selectedWallet = unlockedWallet, isToggleOn = false),
        )
    }

    private fun deepLinkUri(scheme: String, host: String?, pages: List<String>): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
        if (host == null) {
            every { getQueryParameters(any()) } throws UnsupportedOperationException()
        } else {
            every { getQueryParameters("page") } returns pages
        }
    }

    data class SavePageModel(
        val name: String,
        val scheme: String = "tangem",
        val host: String? = "promo_page",
        val pages: List<String> = listOf(PAGE),
        val isToggleOn: Boolean = true,
        val expected: String?,
    ) {
        override fun toString(): String = name
    }

    data class WaitingPageModel(
        val name: String,
        val selectedWallet: UserWallet?,
        val route: AppRoute = AppRoute.Wallet,
        val isToggleOn: Boolean = true,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val PAGE = "https://promo.tangem.com/prize-draw-status/?success"
        const val OLD_PAGE = "https://promo.tangem.com/earlier/"
    }
}