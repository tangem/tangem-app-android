package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesWalletBlockConverterTest {

    private val clickIntents: WalletClickIntents = mockk(relaxed = true)

    private val converter = CollectiblesWalletBlockConverter(
        appCurrency = AppCurrency.Default,
        clickIntents = clickIntents,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: BalanceModel) {
        // Act
        val actual = converter.convert(value = model.status.toAccountStatus()) as CollectiblesWalletBlockUM.Content

        // Assert
        assertThat(actual.balance).isInstanceOf(model.expected)
    }

    @Test
    fun `GIVEN a converted row WHEN clicked THEN the collectibles account of that wallet is opened`() {
        // Arrange
        val actual = converter.convert(value = onboarded().toAccountStatus()) as CollectiblesWalletBlockUM.Content

        // Act
        actual.onClick?.invoke()

        // Assert
        verify(exactly = 1) { clickIntents.onCollectiblesAccountClick(userWalletId) }
    }

    @Test
    fun `GIVEN a missed derivation WHEN convert THEN the row shows a dash and does not open`() {
        // Act
        val actual = converter.convert(
            value = CollectiblesAccountStatusValue.MissedDerivation.toAccountStatus(),
        ) as CollectiblesWalletBlockUM.Content

        // Assert
        assertThat(actual.balance).isEqualTo(CollectiblesWalletBlockUM.Balance.Unknown)
        assertThat(actual.onClick).isNull()
    }

    private fun CollectiblesAccountStatusValue.toAccountStatus() = AccountStatus.Collectibles(
        account = Account.Collectibles(userWalletId),
        value = this,
    )

    private fun onboarded(vaultBalance: CollectiblesVaultBalance? = pricedBalance) =
        CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = vaultBalance)

    internal data class BalanceModel(val status: CollectiblesAccountStatusValue, val expected: Class<*>)

    private fun provideTestModels() = listOf(
        // loading shimmers like the wallet's own account rows; everything else unknown is a dash
        BalanceModel(status = CollectiblesAccountStatusValue.Loading, expected = loading),
        BalanceModel(status = CollectiblesAccountStatusValue.NotOnboarded, expected = unknown),
        BalanceModel(status = CollectiblesAccountStatusValue.Error.Unavailable, expected = unknown),
        BalanceModel(status = CollectiblesAccountStatusValue.MissedDerivation, expected = unknown),
        // the vault exists but has not been read on this device, so there is no amount to show
        BalanceModel(status = onboarded(vaultBalance = null), expected = unknown),
        // something is in the vault and nothing can price it, so there is still no amount to show
        BalanceModel(
            status = onboarded(vaultBalance = pricedBalance.copy(usdcFiatRate = null)),
            expected = unknown,
        ),
        BalanceModel(status = onboarded(), expected = amount),
        BalanceModel(
            status = CollectiblesAccountStatusValue.UpdateRequired(
                source = StatusSource.ACTUAL,
                vaultBalance = pricedBalance,
            ),
            expected = amount,
        ),
    )

    private companion object {
        val userWalletId = UserWalletId("011")
        val loading = CollectiblesWalletBlockUM.Balance.Loading::class.java
        val unknown = CollectiblesWalletBlockUM.Balance.Unknown::class.java
        val amount = CollectiblesWalletBlockUM.Balance.Amount::class.java

        val pricedBalance = MockCollectibles.vaultBalance(
            amount = BigDecimal("2"),
            fiatRate = BigDecimal("150"),
        )
    }
}