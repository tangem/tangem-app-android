package com.tangem.feature.wallet.presentation.wallet.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletAdditionalInfoFactoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isSocialLogin(model: IsSocialLoginModel) {
        // Act
        val actual = WalletAdditionalInfoFactory.resolve(wallet = model.userWallet)

        // Assert
        assertThat(actual.isSocialLogin).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        IsSocialLoginModel(name = "social hot", userWallet = hotWallet(isSocialBackupCompleted = true), expected = true),
        IsSocialLoginModel(
            name = "locked social hot",
            userWallet = hotWallet(isSocialBackupCompleted = true, isLocked = true),
            expected = true,
        ),
        IsSocialLoginModel(name = "plain hot", userWallet = hotWallet(isSocialBackupCompleted = false), expected = false),
        IsSocialLoginModel(name = "cold", userWallet = MockUserWalletFactory.create(), expected = false),
    )

    private fun hotWallet(isSocialBackupCompleted: Boolean, isLocked: Boolean = false) = UserWallet.Hot(
        name = "Mobile wallet",
        walletId = UserWalletId(stringValue = "011"),
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = if (isLocked) null else emptyList(),
        isBackedUp = false,
        isSocialBackupCompleted = isSocialBackupCompleted,
    )

    internal data class IsSocialLoginModel(val name: String, val userWallet: UserWallet, val expected: Boolean) {
        override fun toString(): String = name
    }
}