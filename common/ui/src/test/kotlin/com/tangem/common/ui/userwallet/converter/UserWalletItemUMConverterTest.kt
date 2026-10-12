package com.tangem.common.ui.userwallet.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class UserWalletItemUMConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TitleBadge {

        @ParameterizedTest
        @ProvideTestModels
        fun titleBadge(model: TitleBadgeModel) {
            // Arrange
            val converter = UserWalletItemUMConverter(onClick = {})

            // Act
            val actual = converter.convert(model.userWallet)

            // Assert
            assertThat(actual.titleBadge).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TitleBadgeModel(userWallet = hotWallet(backedUp = true), expected = SEED_BADGE),
            TitleBadgeModel(userWallet = hotWallet(backedUp = false), expected = null),
            TitleBadgeModel(userWallet = hotWallet(backedUp = true, isLocked = true), expected = SEED_BADGE),
            TitleBadgeModel(userWallet = hotWallet(backedUp = false, isLocked = true), expected = null),
            TitleBadgeModel(userWallet = coldWallet, expected = SEED_BADGE),
            TitleBadgeModel(userWallet = seedlessColdWallet, expected = null),
            TitleBadgeModel(userWallet = lockedColdWallet, expected = null),
            TitleBadgeModel(userWallet = singleCurrencyColdWallet, expected = SEED_BADGE),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Information {

        @ParameterizedTest
        @ProvideTestModels
        fun information(model: InformationModel) {
            // Arrange
            val converter = UserWalletItemUMConverter(onClick = {}, mode = model.mode)

            // Act
            val actual = converter.convert(model.userWallet)

            // Assert
            assertThat(actual.information).isEqualTo(UserWalletItemUM.Information.Loaded(model.expected))
        }

        private fun provideTestModels() = listOf(
            InformationModel(
                userWallet = hotWallet(),
                mode = UserWalletItemUMConverter.InfoField.Devices,
                expected = resourceReference(R.string.hw_mobile_wallet),
            ),
            InformationModel(
                userWallet = hotWallet(),
                mode = UserWalletItemUMConverter.InfoField.Tokens(tokensCount = 7),
                expected = resourceReference(R.string.hw_mobile_wallet),
            ),
            InformationModel(
                userWallet = coldWallet,
                mode = UserWalletItemUMConverter.InfoField.Devices,
                expected = cardCountReference(count = 1),
            ),
            InformationModel(
                userWallet = coldWalletWithBackup(cardCount = 2),
                mode = UserWalletItemUMConverter.InfoField.Devices,
                expected = cardCountReference(count = 3),
            ),
            InformationModel(
                userWallet = singleCurrencyColdWallet,
                mode = UserWalletItemUMConverter.InfoField.Devices,
                expected = cardCountReference(count = 1),
            ),
            InformationModel(
                userWallet = coldWallet,
                mode = UserWalletItemUMConverter.InfoField.Tokens(tokensCount = 7),
                expected = pluralReference(
                    id = R.plurals.common_tokens_count,
                    count = 7,
                    formatArgs = wrappedList(7),
                ),
            ),
        )

        private fun cardCountReference(count: Int) = pluralReference(
            id = R.plurals.card_label_card_count,
            count = count,
            formatArgs = wrappedList(count),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Balance {

        @ParameterizedTest
        @ProvideTestModels
        fun balance(model: BalanceModel) {
            // Arrange
            val converter = UserWalletItemUMConverter(
                onClick = {},
                appCurrency = model.appCurrency,
                balance = model.balance,
                isBalanceHidden = model.isBalanceHidden,
                isAuthMode = model.isAuthMode,
                isClickableIfLocked = true,
            )

            // Act
            val actual = converter.convert(model.userWallet)

            // Assert
            assertThat(actual.balance).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            BalanceModel(
                userWallet = hotWallet(isLocked = true),
                balance = loadedBalance(),
                expected = UserWalletItemUM.Balance.Locked,
            ),
            BalanceModel(
                userWallet = lockedColdWallet,
                balance = loadedBalance(),
                expected = UserWalletItemUM.Balance.Locked,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = loadedBalance(),
                isAuthMode = true,
                isBalanceHidden = true,
                expected = UserWalletItemUM.Balance.NotShowing,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = loadedBalance(),
                isBalanceHidden = true,
                expected = UserWalletItemUM.Balance.Hidden,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = null,
                expected = UserWalletItemUM.Balance.Loading,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = TotalFiatBalance.Loading,
                expected = UserWalletItemUM.Balance.Loading,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = TotalFiatBalance.Failed,
                expected = UserWalletItemUM.Balance.Failed,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = loadedBalance(),
                appCurrency = null,
                expected = UserWalletItemUM.Balance.Failed,
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = loadedBalance(source = StatusSource.ACTUAL),
                expected = UserWalletItemUM.Balance.Loaded(value = FORMATTED_BALANCE, isFlickering = false),
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = loadedBalance(source = StatusSource.CACHE),
                expected = UserWalletItemUM.Balance.Loaded(value = FORMATTED_BALANCE, isFlickering = true),
            ),
            BalanceModel(
                userWallet = hotWallet(),
                balance = loadedBalance(source = StatusSource.ONLY_CACHE),
                expected = UserWalletItemUM.Balance.Loaded(value = FORMATTED_BALANCE, isFlickering = false),
            ),
        )

        private fun loadedBalance(source: StatusSource = StatusSource.ACTUAL) = TotalFiatBalance.Loaded(
            amount = BigDecimal("16.99"),
            source = source,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsEnabled {

        @ParameterizedTest
        @ProvideTestModels
        fun isEnabled(model: IsEnabledModel) {
            // Arrange
            val converter = UserWalletItemUMConverter(
                onClick = {},
                isAuthMode = model.isAuthMode,
                isClickableIfLocked = model.isClickableIfLocked,
            )

            // Act
            val actual = converter.convert(model.userWallet)

            // Assert
            assertThat(actual.isEnabled).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            IsEnabledModel(userWallet = hotWallet(), expected = true),
            IsEnabledModel(userWallet = coldWallet, expected = true),
            IsEnabledModel(userWallet = hotWallet(isLocked = true), expected = false),
            IsEnabledModel(userWallet = lockedColdWallet, expected = false),
            IsEnabledModel(userWallet = hotWallet(isLocked = true), isAuthMode = true, expected = true),
            IsEnabledModel(userWallet = lockedColdWallet, isClickableIfLocked = true, expected = true),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Passthrough {

        @Test
        fun `GIVEN defaults WHEN convert THEN identity fields mapped and image is loading`() {
            // Arrange
            val userWallet = hotWallet()
            val converter = UserWalletItemUMConverter(onClick = {})

            // Act
            val actual = converter.convert(userWallet)

            // Assert
            assertThat(actual.id).isEqualTo(userWallet.walletId.stringValue)
            assertThat(actual.name).isEqualTo(stringReference(userWallet.name))
            assertThat(actual.endIcon).isEqualTo(UserWalletItemUM.EndIcon.None)
            assertThat(actual.imageState).isEqualTo(UserWalletItemUM.ImageState.Loading)
        }

        @Test
        fun `GIVEN end icon and artwork WHEN convert THEN both passed through`() {
            // Arrange
            val artwork = UserWalletItemUM.ImageState.MobileWallet
            val converter = UserWalletItemUMConverter(
                onClick = {},
                endIcon = UserWalletItemUM.EndIcon.Warning,
                artwork = artwork,
            )

            // Act
            val actual = converter.convert(hotWallet())

            // Assert
            assertThat(actual.endIcon).isEqualTo(UserWalletItemUM.EndIcon.Warning)
            assertThat(actual.imageState).isEqualTo(artwork)
        }

        @Test
        fun `GIVEN converted item WHEN onClick invoked THEN wallet id emitted`() {
            // Arrange
            val userWallet = hotWallet()
            val clicked = mutableListOf<UserWalletId>()
            val converter = UserWalletItemUMConverter(onClick = clicked::add)

            // Act
            converter.convert(userWallet).onClick()

            // Assert
            assertThat(clicked).containsExactly(userWallet.walletId)
        }
    }

    internal data class TitleBadgeModel(val userWallet: UserWallet, val expected: TangemBadgeUM?)

    internal data class InformationModel(
        val userWallet: UserWallet,
        val mode: UserWalletItemUMConverter.InfoField,
        val expected: TextReference,
    )

    internal data class BalanceModel(
        val userWallet: UserWallet,
        val balance: TotalFiatBalance?,
        val appCurrency: AppCurrency? = AppCurrency.Default,
        val isBalanceHidden: Boolean = false,
        val isAuthMode: Boolean = false,
        val expected: UserWalletItemUM.Balance,
    )

    internal data class IsEnabledModel(
        val userWallet: UserWallet,
        val isAuthMode: Boolean = false,
        val isClickableIfLocked: Boolean = false,
        val expected: Boolean,
    )

    private companion object {

        const val FORMATTED_BALANCE = "$16.99"

        val SEED_BADGE = TangemBadgeUM(
            text = resourceReference(R.string.details_wallet_seed_badge),
            size = TangemBadge.Size.X4,
        )

        val coldWallet = MockUserWalletFactory.create()

        val singleCurrencyColdWallet = MockUserWalletFactory.createSingleWalletWithToken()

        val seedlessColdWallet = coldWallet.copy(
            scanResponse = coldWallet.scanResponse.copy(
                card = coldWallet.scanResponse.card.copy(
                    wallets = coldWallet.scanResponse.card.wallets.map { it.copy(isImported = false) },
                ),
            ),
        )

        val lockedColdWallet = coldWallet.copy(
            scanResponse = coldWallet.scanResponse.copy(
                card = coldWallet.scanResponse.card.copy(wallets = emptyList()),
            ),
        )

        fun coldWalletWithBackup(cardCount: Int) = coldWallet.copy(
            scanResponse = coldWallet.scanResponse.copy(
                card = coldWallet.scanResponse.card.copy(
                    backupStatus = CardDTO.BackupStatus.Active(cardCount = cardCount),
                ),
            ),
        )

        fun hotWallet(backedUp: Boolean = false, isLocked: Boolean = false) = UserWallet.Hot(
            name = "Mobile wallet",
            walletId = UserWalletId(stringValue = "011"),
            hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
            wallets = if (isLocked) null else emptyList(),
            backedUp = backedUp,
        )
    }
}