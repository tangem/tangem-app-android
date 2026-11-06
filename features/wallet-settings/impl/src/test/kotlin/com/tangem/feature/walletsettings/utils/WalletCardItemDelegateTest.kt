package com.tangem.feature.walletsettings.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletCardItemDelegateTest {

    private val walletImageFetcher: UserWalletImageFetcher = mockk {
        every { walletImage(any<UserWallet>(), ArtworkSize.SMALL) } returns
            flowOf(UserWalletItemUM.ImageState.MobileWallet)
    }

    private val delegate = WalletCardItemDelegate(
        walletImageFetcher = walletImageFetcher,
        dialogNavigation = mockk(relaxed = true),
        onUpgradeHotWalletClick = {},
    )

    @ParameterizedTest
    @ProvideTestModels
    fun isSocialLogin(model: IsSocialLoginModel) = runTest {
        // Act
        val actual = delegate.cardItemFlow(model.userWallet).first()

        // Assert
        assertThat(actual.isSocialLogin).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        IsSocialLoginModel(name = "social hot", userWallet = hotWallet(isSocialBackupCompleted = true), expected = true),
        IsSocialLoginModel(name = "plain hot", userWallet = hotWallet(isSocialBackupCompleted = false), expected = false),
        IsSocialLoginModel(name = "cold", userWallet = MockUserWalletFactory.create(), expected = false),
    )

    private fun hotWallet(isSocialBackupCompleted: Boolean) = UserWallet.Hot(
        name = "Mobile wallet",
        walletId = UserWalletId(stringValue = "011"),
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = emptyList(),
        isBackedUp = false,
        isSocialBackupCompleted = isSocialBackupCompleted,
    )

    internal data class IsSocialLoginModel(val name: String, val userWallet: UserWallet, val expected: Boolean) {
        override fun toString(): String = name
    }
}