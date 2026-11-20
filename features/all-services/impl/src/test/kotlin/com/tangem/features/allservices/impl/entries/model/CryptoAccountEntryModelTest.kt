package com.tangem.features.allservices.impl.entries.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CryptoAccountEntryModelTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, userWalletsListRepository)
        every { userWalletsListRepository.userWallets } returns userWallets
    }

    @Test
    fun `GIVEN an unlocked hot wallet WHEN model created THEN visible from the start`() = runTest {
        // Arrange
        userWallets.value = listOf(hotWallet(isLocked = false))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN only a locked wallet WHEN model created THEN hidden`() = runTest {
        // Arrange
        userWallets.value = listOf(hotWallet(isLocked = true))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry shown WHEN clicked THEN crypto account intro opens`() = runTest {
        // Arrange
        userWallets.value = listOf(hotWallet(isLocked = false))
        val model = createModel()

        // Act
        model.onClick()

        // Assert
        verify(exactly = 1) { router.push(AppRoute.CryptoAccountIntro) }
        model.onDestroy()
    }

    private fun TestScope.createModel() = CryptoAccountEntryModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        userWalletsListRepository = userWalletsListRepository,
    )

    private fun hotWallet(isLocked: Boolean) = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId("011"),
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = if (isLocked) null else emptyList(),
        isBackedUp = false,
    )
}