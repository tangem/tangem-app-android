package com.tangem.domain.wallets.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletsTypeAnalyticsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN listed wallets WHEN toAnalyticsWalletsType THEN returns the wallets type`(model: WalletsTypeModel) {
        // Act
        val actual = model.userWallets.toAnalyticsWalletsType()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        WalletsTypeModel(description = "not loaded", userWallets = null, expected = null),
        WalletsTypeModel(description = "empty", userWallets = emptyList(), expected = null),
        WalletsTypeModel(
            description = "only mobile",
            userWallets = listOf(hotWallet(), hotWallet()),
            expected = AnalyticsParam.WalletsType.Mobile,
        ),
        WalletsTypeModel(
            description = "only cold",
            userWallets = listOf(coldWallet()),
            expected = AnalyticsParam.WalletsType.Cold,
        ),
        WalletsTypeModel(
            description = "mobile and cold",
            userWallets = listOf(coldWallet(), hotWallet()),
            expected = AnalyticsParam.WalletsType.Multiple,
        ),
    )

    private fun hotWallet(): UserWallet = mockk<UserWallet.Hot>()

    private fun coldWallet(): UserWallet = mockk<UserWallet.Cold>()

    data class WalletsTypeModel(
        val description: String,
        val userWallets: List<UserWallet>?,
        val expected: AnalyticsParam.WalletsType?,
    ) {
        override fun toString(): String = description
    }
}