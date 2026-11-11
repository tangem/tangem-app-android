package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.MAX_HOT_WALLETS_COUNT
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetHotWalletAdditionRestrictionUseCaseTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private var isMultipleHotWalletsEnabled = true

    private val useCase = GetHotWalletAdditionRestrictionUseCase(
        userWalletsListRepository = userWalletsListRepository,
        isMultipleHotWalletsEnabled = { isMultipleHotWalletsEnabled },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN listed wallets WHEN invoke THEN returns expected restriction`(model: RestrictionModel) = runTest {
        // Arrange
        isMultipleHotWalletsEnabled = model.isMultipleHotWalletsEnabled
        coEvery { userWalletsListRepository.userWalletsSync() } returns model.userWallets

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN multiple hot wallets disabled WHEN invoke THEN the wallets list is not read`() = runTest {
        // Arrange
        isMultipleHotWalletsEnabled = false

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { userWalletsListRepository.userWalletsSync() }
    }

    private fun provideTestModels() = listOf(
        RestrictionModel(
            description = "no wallets",
            userWallets = emptyList(),
            expected = null,
        ),
        RestrictionModel(
            description = "hot wallets below the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT - 1),
            expected = null,
        ),
        RestrictionModel(
            description = "hot wallets at the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT),
            expected = HotWalletAdditionRestriction.LimitReached,
        ),
        RestrictionModel(
            description = "hot wallets above the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT + 1),
            expected = HotWalletAdditionRestriction.LimitReached,
        ),
        RestrictionModel(
            description = "cold wallets do not count towards the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT - 1) + List(3) { mockk<UserWallet.Cold>() },
            expected = null,
        ),
        RestrictionModel(
            description = "hot wallets at the limit with multiple hot wallets disabled",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT),
            expected = null,
            isMultipleHotWalletsEnabled = false,
        ),
    )

    private fun hotWallets(count: Int): List<UserWallet> = List(count) { hotWallet(index = it) }

    private fun hotWallet(index: Int): UserWallet.Hot = UserWallet.Hot(
        name = "Hot $index",
        walletId = UserWalletId("%04x".format(index)),
        hotWalletId = HotWalletId(value = "hot_$index", authType = HotWalletId.AuthType.NoPassword),
        wallets = null,
        isBackedUp = false,
    )

    data class RestrictionModel(
        val description: String,
        val userWallets: List<UserWallet>,
        val expected: HotWalletAdditionRestriction?,
        val isMultipleHotWalletsEnabled: Boolean = true,
    ) {
        override fun toString(): String = description
    }
}