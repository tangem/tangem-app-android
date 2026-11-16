package com.tangem.common.ui.account

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountCollectiblesItemUMConverterTest {

    private val account = Account.Collectibles(userWalletId = UserWalletId("011"))
    private val appCurrency = AppCurrency(name = "US Dollar", code = "USD", symbol = "$")

    @Test
    fun `GIVEN a collectibles account WHEN convert THEN the row is a disabled collectibles row without info`() {
        // Act
        val actual = AccountCollectiblesItemUMConverter(appCurrency = appCurrency)
            .convert(status(CollectiblesAccountStatusValue.Loading))

        // Assert
        val name = resourceReference(R.string.collectibles_main_title)
        assertThat(actual.id).isEqualTo(account.accountId.value)
        assertThat(actual.name).isEqualTo(name)
        assertThat(actual.imageState).isEqualTo(
            UserWalletItemUM.ImageState.Account(name = name, icon = AccountIconUM.Collectibles),
        )
        assertThat(actual.information).isEqualTo(UserWalletItemUM.Information.None)
        assertThat(actual.isEnabled).isFalse()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a collectibles status WHEN convert THEN the balance matches it`(model: BalanceModel) {
        // Act
        val actual = AccountCollectiblesItemUMConverter(appCurrency = appCurrency, isBalanceHidden = model.isHidden)
            .convert(status(model.value))

        // Assert
        assertThat(actual.balance).isEqualTo(model.expected)
    }

    private fun status(value: CollectiblesAccountStatusValue) = AccountStatus.Collectibles(
        account = account,
        value = value,
    )

    internal data class BalanceModel(
        val value: CollectiblesAccountStatusValue,
        val isHidden: Boolean = false,
        val expected: UserWalletItemUM.Balance,
    )

    private fun provideTestModels() = listOf(
        BalanceModel(value = CollectiblesAccountStatusValue.Loading, expected = UserWalletItemUM.Balance.Loading),
        BalanceModel(
            value = CollectiblesAccountStatusValue.MissedDerivation,
            expected = UserWalletItemUM.Balance.Failed,
        ),
        BalanceModel(
            value = CollectiblesAccountStatusValue.Error.Unavailable,
            expected = UserWalletItemUM.Balance.Failed,
        ),
        // A vault not read yet: a dash, not the zero the wallet total counts it as
        BalanceModel(
            value = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
            expected = UserWalletItemUM.Balance.Failed,
        ),
        BalanceModel(
            value = CollectiblesAccountStatusValue.Loading,
            isHidden = true,
            expected = UserWalletItemUM.Balance.Hidden,
        ),
    )
}