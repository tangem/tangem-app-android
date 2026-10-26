package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountState
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFromUM
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawAmountUMFactoryTest {

    private val userWalletId = UserWalletId("011")
    private val onSourceClick: () -> Unit = {}
    private val onRouteClick: () -> Unit = {}
    private val callbacks = TangemPayBankWithdrawAmountUMFactory.Callbacks(
        onBackClick = {},
        onCloseClick = {},
        onValueChange = {},
        onMaxClick = {},
        onSourceClick = onSourceClick,
        onSourceSelected = {},
        onRouteClick = onRouteClick,
        onRouteSelected = {},
        onSheetDismiss = {},
        onContinueClick = {},
    )
    private val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)
    private val selection = AmountState().resolve(draft = draft, options = bankWithdrawOptions())

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN source account WHEN create THEN from row and token choice follow the account`(model: AccountModel) {
        // Arrange
        val factory = createFactory(model.account)

        // Act
        val actual = factory.create(draft = draft, selection = selection, sheet = null, isBalanceHidden = false)

        // Assert
        assertThat(actual.from).isEqualTo(model.expectedFrom)
        assertThat(actual.source.onClick).isEqualTo(model.expectedSourceClick)
    }

    private fun provideTestModels() = listOf(
        AccountModel(
            account = BankWithdrawSourceAccount.Payment(userWalletId),
            expectedFrom = BankWithdrawFromUM(
                title = resourceReference(R.string.tangempay_payment_account),
                icon = AccountIconUM.Payment,
            ),
            expectedSourceClick = onSourceClick,
        ),
        AccountModel(
            account = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1"),
            expectedFrom = BankWithdrawFromUM(
                title = resourceReference(R.string.money_account_main_name),
                icon = AccountIconUM.Money,
            ),
            expectedSourceClick = null,
        ),
    )

    internal data class AccountModel(
        val account: BankWithdrawSourceAccount,
        val expectedFrom: BankWithdrawFromUM,
        val expectedSourceClick: (() -> Unit)?,
    )

    @Test
    fun `GIVEN money account with two rails WHEN create THEN rail row opens the route sheet`() {
        // Arrange
        val factory = createFactory(BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1"))

        // Act
        val actual = factory.create(draft = draft, selection = selection, sheet = null, isBalanceHidden = false)

        // Assert
        assertThat(actual.route.onClick).isEqualTo(onRouteClick)
    }

    private fun createFactory(account: BankWithdrawSourceAccount) = TangemPayBankWithdrawAmountUMFactory(
        step = TangemPayBankWithdrawInnerRoute.Amount,
        sourceAccount = account,
        callbacks = callbacks,
    )
}