package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.domain.NotificationsBalanceState
import com.tangem.feature.wallet.presentation.wallet.state.model.JointOperationBannerUM
import com.tangem.feature.wallet.presentation.wallet.state.model.PromoCardsCheck
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletType
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.state.model.createWalletContentUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class SetWarningsTransformerTest {

    @Test
    fun `GIVEN no balance state WHEN transform THEN notifications are set as they are`() {
        // Arrange
        val walletUM = createWalletContentUM()
        val transformer = createTransformer(
            notifications = persistentListOf(missingBackup, tangemPayPromo),
            notificationsCarousel = persistentListOf(rateApp),
            balanceState = null,
        )

        // Act
        val actual = transformer.transform(walletUM)

        // Assert
        val expected = walletUM.copy(
            notifications = persistentListOf(missingBackup, tangemPayPromo),
            notificationsCarousel = persistentListOf(rateApp),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a locked wallet WHEN transform with a balance state THEN it stays as it is`() {
        // Arrange
        val walletUM = WalletUM.Locked(
            walletsBalanceUM = WalletBalanceUM.Loading(
                id = UserWalletId("01"),
                name = "test",
                deviceIcon = DeviceIconUM.Mobile,
            ),
            buttons = persistentListOf(),
            type = WalletType.Cold,
        )
        val transformer = createTransformer(
            notifications = persistentListOf(tangemPayPromo),
            balanceState = NotificationsBalanceState.Settled,
        )

        // Act
        val actual = transformer.transform(walletUM)

        // Assert
        assertThat(actual).isEqualTo(walletUM)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PromoCardsCheckWithoutSystemCards {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN hidden promo cards and a check WHEN transform THEN they come back once the check is due`(
            model: CheckModel,
        ) {
            // Arrange
            val walletUM = createWalletContentUM(arePromoCardsVisible = false, promoCardsCheck = model.check)
            val transformer = createTransformer(
                notifications = persistentListOf(tangemPayPromo),
                balanceState = model.balanceState,
            )

            // Act
            val actual = transformer.transform(walletUM)

            // Assert
            val expected = createWalletContentUM(
                notifications = if (model.areShown) persistentListOf(tangemPayPromo) else persistentListOf(),
                arePromoCardsVisible = model.areShown,
                promoCardsCheck = model.expectedCheck,
            )
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            CheckModel(
                check = PromoCardsCheck.OnOpen,
                balanceState = NotificationsBalanceState.Loading,
                areShown = false,
                expectedCheck = PromoCardsCheck.OnOpen,
            ),
            CheckModel(
                check = PromoCardsCheck.OnOpen,
                balanceState = NotificationsBalanceState.Resolved,
                areShown = true,
                expectedCheck = PromoCardsCheck.None,
            ),
            CheckModel(
                check = PromoCardsCheck.OnOpen,
                balanceState = NotificationsBalanceState.Settled,
                areShown = true,
                expectedCheck = PromoCardsCheck.None,
            ),
            CheckModel(
                check = PromoCardsCheck.AfterRefresh,
                balanceState = NotificationsBalanceState.Loading,
                areShown = false,
                expectedCheck = PromoCardsCheck.AfterRefresh,
            ),
            CheckModel(
                check = PromoCardsCheck.AfterRefresh,
                balanceState = NotificationsBalanceState.Resolved,
                areShown = false,
                expectedCheck = PromoCardsCheck.AfterRefresh,
            ),
            CheckModel(
                check = PromoCardsCheck.AfterRefresh,
                balanceState = NotificationsBalanceState.Settled,
                areShown = true,
                expectedCheck = PromoCardsCheck.None,
            ),
            CheckModel(
                check = PromoCardsCheck.None,
                balanceState = NotificationsBalanceState.Settled,
                areShown = false,
                expectedCheck = PromoCardsCheck.None,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SystemCards {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN shown promo cards WHEN a system card is set THEN they hide at once`(model: SystemCardModel) {
            // Arrange
            val walletUM = createWalletContentUM(
                jointOperationBanners = model.jointOperationBanners,
                arePromoCardsVisible = true,
                promoCardsCheck = PromoCardsCheck.None,
            )
            val transformer = createTransformer(
                notifications = persistentListOf(*model.notifications.toTypedArray(), tangemPayPromo),
                notificationsCarousel = model.notificationsCarousel,
                balanceState = NotificationsBalanceState.Settled,
            )

            // Act
            val actual = transformer.transform(walletUM)

            // Assert
            val expected = walletUM.copy(
                notifications = model.notifications,
                notificationsCarousel = model.notificationsCarousel,
                arePromoCardsVisible = false,
            )
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            SystemCardModel(notifications = persistentListOf(missingBackup)),
            SystemCardModel(notificationsCarousel = persistentListOf(rateApp)),
            SystemCardModel(jointOperationBanners = persistentListOf(jointOperationBanner)),
        )

        @Test
        fun `GIVEN a due check WHEN a system card is set THEN promo cards stay hidden and the check is done`() {
            // Arrange
            val walletUM = createWalletContentUM(arePromoCardsVisible = false, promoCardsCheck = PromoCardsCheck.OnOpen)
            val transformer = createTransformer(
                notifications = persistentListOf(missingBackup, tangemPayPromo),
                balanceState = NotificationsBalanceState.Resolved,
            )

            // Act
            val actual = transformer.transform(walletUM)

            // Assert
            val expected = createWalletContentUM(
                notifications = persistentListOf(missingBackup),
                arePromoCardsVisible = false,
                promoCardsCheck = PromoCardsCheck.None,
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN an open check and a system card WHEN the balance is loading THEN the check waits`() {
            // Arrange
            val walletUM = createWalletContentUM(arePromoCardsVisible = false, promoCardsCheck = PromoCardsCheck.OnOpen)
            val transformer = createTransformer(
                notifications = persistentListOf(missingBackup, tangemPayPromo),
                balanceState = NotificationsBalanceState.Loading,
            )

            // Act
            val actual = transformer.transform(walletUM)

            // Assert
            val expected = createWalletContentUM(
                notifications = persistentListOf(missingBackup),
                arePromoCardsVisible = false,
                promoCardsCheck = PromoCardsCheck.OnOpen,
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN shown promo cards WHEN only Tangem Pay warnings are set THEN promo cards stay`() {
            // Arrange
            val walletUM = createWalletContentUM(arePromoCardsVisible = true, promoCardsCheck = PromoCardsCheck.None)
            val notifications = persistentListOf(tangemPayRefreshNeeded, tangemPayUnreachable, tangemPayPromo)
            val transformer = createTransformer(
                notifications = notifications,
                balanceState = NotificationsBalanceState.Settled,
            )

            // Act
            val actual = transformer.transform(walletUM)

            // Assert
            assertThat(actual).isEqualTo(walletUM.copy(notifications = notifications))
        }
    }

    private fun createTransformer(
        notifications: ImmutableList<WalletNotificationUM> = persistentListOf(),
        notificationsCarousel: ImmutableList<WalletNotificationUM> = persistentListOf(),
        balanceState: NotificationsBalanceState?,
    ) = SetWarningsTransformer(
        userWalletId = UserWalletId("01"),
        warnings = persistentListOf(),
        notifications = notifications,
        notificationsCarousel = notificationsCarousel,
        promoCardsBalanceState = balanceState,
    )

    data class CheckModel(
        val check: PromoCardsCheck,
        val balanceState: NotificationsBalanceState,
        val areShown: Boolean,
        val expectedCheck: PromoCardsCheck,
    )

    data class SystemCardModel(
        val notifications: ImmutableList<WalletNotificationUM> = persistentListOf(),
        val notificationsCarousel: ImmutableList<WalletNotificationUM> = persistentListOf(),
        val jointOperationBanners: ImmutableList<JointOperationBannerUM> = persistentListOf(),
    )

    private companion object {
        val tangemPayPromo = WalletNotificationUM.TangemPayPromo(onLaterClick = {}, onLearnMoreClick = {})
        val tangemPayRefreshNeeded = WalletNotificationUM.TangemPayRefreshNeeded(
            onRefreshClick = {},
            buttonText = stringReference("Refresh"),
            shouldShowProgress = false,
        )
        val tangemPayUnreachable = WalletNotificationUM.TangemPayUnreachable
        val missingBackup = WalletNotificationUM.MissingBackup(onClick = {})
        val rateApp = WalletNotificationUM.RateApp(onLikeClick = {}, onDislikeClick = {}, onCloseClick = {})
        val jointOperationBanner = JointOperationBannerUM(operationId = "operation", banner = mockk())
    }
}