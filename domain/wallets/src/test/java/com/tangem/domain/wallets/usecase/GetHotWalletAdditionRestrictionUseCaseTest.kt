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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
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
        val actual = useCase(isSocialLogin = model.isSocialLogin)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        RestrictionModel(
            description = "no wallets",
            userWallets = emptyList(),
            isSocialLogin = false,
            expected = null,
        ),
        RestrictionModel(
            description = "hot wallets below the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT - 1),
            isSocialLogin = false,
            expected = null,
        ),
        RestrictionModel(
            description = "hot wallets at the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT),
            isSocialLogin = false,
            expected = HotWalletAdditionRestriction.LimitReached,
        ),
        RestrictionModel(
            description = "hot wallets above the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT + 1),
            isSocialLogin = false,
            expected = HotWalletAdditionRestriction.LimitReached,
        ),
        RestrictionModel(
            description = "cold wallets do not count towards the limit",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT - 1) + List(3) { mockk<UserWallet.Cold>() },
            isSocialLogin = false,
            expected = null,
        ),
        RestrictionModel(
            description = "limit takes precedence over the social wallet",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT - 1) + hotWallet(index = 99, isSocial = true),
            isSocialLogin = true,
            expected = HotWalletAdditionRestriction.LimitReached,
        ),
        RestrictionModel(
            description = "social login with a social wallet listed",
            userWallets = hotWallets(1) + hotWallet(index = 99, isSocial = true),
            isSocialLogin = true,
            expected = HotWalletAdditionRestriction.SocialWalletAlreadyAdded,
        ),
        RestrictionModel(
            description = "social login without a social wallet listed",
            userWallets = hotWallets(2),
            isSocialLogin = true,
            expected = null,
        ),
        RestrictionModel(
            description = "hot wallets at the limit with multiple hot wallets disabled",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT),
            isSocialLogin = false,
            expected = null,
            isMultipleHotWalletsEnabled = false,
        ),
        RestrictionModel(
            description = "social login at the limit with multiple hot wallets disabled and a social wallet listed",
            userWallets = hotWallets(MAX_HOT_WALLETS_COUNT - 1) + hotWallet(index = 99, isSocial = true),
            isSocialLogin = true,
            expected = HotWalletAdditionRestriction.SocialWalletAlreadyAdded,
            isMultipleHotWalletsEnabled = false,
        ),
        RestrictionModel(
            description = "regular creation with a social wallet listed",
            userWallets = hotWallets(1) + hotWallet(index = 99, isSocial = true),
            isSocialLogin = false,
            expected = null,
        ),
    )

    private fun hotWallets(count: Int): List<UserWallet> = List(count) { hotWallet(index = it) }

    private fun hotWallet(index: Int, isSocial: Boolean = false): UserWallet.Hot = UserWallet.Hot(
        name = "Hot $index",
        walletId = UserWalletId("%04x".format(index)),
        hotWalletId = HotWalletId(value = "hot_$index", authType = HotWalletId.AuthType.NoPassword),
        wallets = null,
        isBackedUp = false,
        isSocialBackupCompleted = isSocial,
    )

    data class RestrictionModel(
        val description: String,
        val userWallets: List<UserWallet>,
        val isSocialLogin: Boolean,
        val expected: HotWalletAdditionRestriction?,
        val isMultipleHotWalletsEnabled: Boolean = true,
    ) {
        override fun toString(): String = description
    }
}