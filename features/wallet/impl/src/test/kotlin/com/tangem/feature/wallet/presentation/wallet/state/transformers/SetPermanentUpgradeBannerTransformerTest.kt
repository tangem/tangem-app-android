package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.PermanentUpgradeBannerUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletType
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.state.model.createWalletContentUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetPermanentUpgradeBannerTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wallet content WHEN banner set THEN the content holds the new banner`(model: ContentModel) {
        // Arrange
        val walletUM = createWalletContentUM(userWalletId = USER_WALLET_ID)
            .copy(permanentUpgradeBannerUM = model.prevBannerUM)

        // Act
        val actual = createTransformer(model.newBannerUM).transform(walletUM)

        // Assert
        val expected = createWalletContentUM(userWalletId = USER_WALLET_ID)
            .copy(permanentUpgradeBannerUM = model.newBannerUM)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN locked wallet WHEN banner set THEN the wallet is unchanged`() {
        // Arrange
        val walletUM = WalletUM.Locked(
            walletsBalanceUM = WalletBalanceUM.Loading(
                id = USER_WALLET_ID,
                name = "test",
                deviceIcon = DeviceIconUM.Mobile,
            ),
            buttons = persistentListOf(),
            type = WalletType.Hot,
        )

        // Act
        val actual = createTransformer(BANNER_UM).transform(walletUM)

        // Assert
        assertThat(actual).isSameInstanceAs(walletUM)
    }

    private fun createTransformer(bannerUM: PermanentUpgradeBannerUM?) = SetPermanentUpgradeBannerTransformer(
        userWalletId = USER_WALLET_ID,
        permanentUpgradeBannerUM = bannerUM,
    )

    private fun provideTestModels() = listOf(
        ContentModel(prevBannerUM = null, newBannerUM = BANNER_UM),
        ContentModel(prevBannerUM = BANNER_UM, newBannerUM = null),
        ContentModel(prevBannerUM = null, newBannerUM = null),
    )

    data class ContentModel(
        val prevBannerUM: PermanentUpgradeBannerUM?,
        val newBannerUM: PermanentUpgradeBannerUM?,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("01")
        val BANNER_UM = PermanentUpgradeBannerUM(onClick = {})
    }
}