package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSheet
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountState
import com.tangem.features.tangempay.withdraw.bank.amount.model.NetworkFeeState
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.Content
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.FeeUnreachable
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.Loading
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFromUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawAmountUMFactoryTest {

    private val userWalletId = UserWalletId("011")
    private val moneyAccount = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1")
    private val moneyAccountFrom = BankWithdrawFromUM(
        title = resourceReference(R.string.money_account_main_name),
        icon = AccountIconUM.Money,
    )
    private val onBackClick: () -> Unit = {}
    private val onCloseClick: () -> Unit = {}
    private val onSourceClick: () -> Unit = {}
    private val onRouteClick: () -> Unit = {}
    private val onRetryFeeClick: () -> Unit = {}
    private val callbacks = TangemPayBankWithdrawAmountUMFactory.Callbacks(
        onBackClick = onBackClick,
        onCloseClick = onCloseClick,
        onValueChange = {},
        onMaxClick = {},
        onSourceClick = onSourceClick,
        onSourceSelected = {},
        onRouteClick = onRouteClick,
        onRouteSelected = {},
        onSheetDismiss = {},
        onContinueClick = {},
        onRetryFeeClick = onRetryFeeClick,
    )
    private val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)
    private val header = BankWithdrawHeaderFactory.create(
        draft = draft,
        step = TangemPayBankWithdrawInnerRoute.Amount,
        onCloseClick = onCloseClick,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN source account WHEN create THEN from row and token choice follow the account`(model: AccountModel) {
        // Arrange
        val factory = createFactory(model.account)

        // Act
        val actual = factory.createWith(networkFee = NetworkFeeState.Loaded(BigDecimal.ZERO)) as Content

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
            account = moneyAccount,
            expectedFrom = moneyAccountFrom,
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
        val factory = createFactory(moneyAccount)

        // Act
        val actual = factory.createWith(networkFee = NetworkFeeState.Loaded(BigDecimal("0.05"))) as Content

        // Assert
        assertThat(actual.route.onClick).isEqualTo(onRouteClick)
    }

    @ParameterizedTest
    @MethodSource("provideRailFeeModels")
    fun `GIVEN network fee WHEN create with the route sheet THEN rail fees include it and the note follows the quote`(
        model: RailFeeModel,
    ) {
        // Arrange
        val factory = createFactory(model.account)

        // Act
        val actual = factory.createWith(networkFee = model.networkFee, sheet = AmountSheet.ROUTES) as Content

        // Assert
        val sheet = actual.sheet as SheetUM.Routes
        assertThat(actual.route.fee).isEqualTo(model.expectedFees.first())
        assertThat(sheet.items.map { it.fee }).containsExactlyElementsIn(model.expectedFees).inOrder()
        assertThat(sheet.networkFeeNote).isEqualTo(model.expectedNote)
    }

    private fun provideRailFeeModels() = listOf(
        RailFeeModel(
            account = BankWithdrawSourceAccount.Payment(userWalletId),
            networkFee = NetworkFeeState.Loaded(BigDecimal.ZERO),
            expectedFees = listOf("$1", "$11"),
            expectedNote = null,
        ),
        RailFeeModel(
            account = moneyAccount,
            networkFee = NetworkFeeState.Loaded(BigDecimal("0.03")),
            expectedFees = listOf("$1.03", "$11.03"),
            expectedNote = resourceReference(R.string.money_account_send_network_fee_note, wrappedList("$0.03")),
        ),
        RailFeeModel(
            account = moneyAccount,
            networkFee = NetworkFeeState.NotEnoughFunds,
            expectedFees = listOf("$1", "$11"),
            expectedNote = null,
        ),
    )

    internal data class RailFeeModel(
        val account: BankWithdrawSourceAccount,
        val networkFee: NetworkFeeState,
        val expectedFees: List<String>,
        val expectedNote: TextReference?,
    )

    @ParameterizedTest
    @MethodSource("provideSpeiRouteModels")
    fun `GIVEN SPEI picked WHEN amount entered THEN the rail row shows one fee and the sheet keeps the formula`(
        model: SpeiRouteModel,
    ) {
        // Arrange
        val factory = createFactory(moneyAccount)
        val networkFee = NetworkFeeState.Loaded(BigDecimal("0.03"))
        val options = bankWithdrawOptions(destinations = listOf(offrampDestination(), SPEI_DESTINATION))
        val state = AmountState(value = model.value, selectedRail = BankRail.SPEI, networkFee = networkFee)

        // Act
        val actual = factory.create(
            draft = draft,
            selection = state.resolve(draft = draft, options = options, isEditing = false),
            sheet = AmountSheet.ROUTES,
            networkFee = networkFee,
            isBalanceHidden = false,
        ) as Content

        // Assert
        val speiItem = (actual.sheet as SheetUM.Routes).items.single { it.rail == BankRail.SPEI }
        assertThat(actual.route.eta).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_eta_within_minutes))
        assertThat(actual.route.fee).isEqualTo(model.expectedRouteFee)
        assertThat(speiItem.fee).isEqualTo("$0.53 + 1%")
    }

    private fun provideSpeiRouteModels() = listOf(
        SpeiRouteModel(value = "", expectedRouteFee = "$0.53 + 1%"),
        SpeiRouteModel(value = "0", expectedRouteFee = "$0.53 + 1%"),
        SpeiRouteModel(value = "500", expectedRouteFee = "$5.53"),
    )

    internal data class SpeiRouteModel(val value: String, val expectedRouteFee: String)

    @ParameterizedTest
    @MethodSource("provideNetworkFeeModels")
    fun `GIVEN network fee not loaded WHEN create THEN placeholder screen instead of the form`(model: NetworkFeeModel) {
        // Arrange
        val factory = createFactory(moneyAccount)

        // Act
        val actual = factory.createWith(networkFee = model.networkFee)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideNetworkFeeModels() = listOf(
        NetworkFeeModel(
            networkFee = NetworkFeeState.Loading,
            expected = Loading(
                header = header,
                from = moneyAccountFrom,
                currencySymbol = "$",
                onBackClick = onBackClick,
            ),
        ),
        NetworkFeeModel(
            networkFee = NetworkFeeState.Unreachable,
            expected = FeeUnreachable(header = header, onRetryClick = onRetryFeeClick, onBackClick = onBackClick),
        ),
    )

    internal data class NetworkFeeModel(
        val networkFee: NetworkFeeState,
        val expected: TangemPayBankWithdrawAmountUM,
    )

    private fun createFactory(account: BankWithdrawSourceAccount) = TangemPayBankWithdrawAmountUMFactory(
        step = TangemPayBankWithdrawInnerRoute.Amount,
        sourceAccount = account,
        callbacks = callbacks,
    )

    private fun TangemPayBankWithdrawAmountUMFactory.createWith(
        networkFee: NetworkFeeState,
        sheet: AmountSheet? = null,
    ) = create(
        draft = draft,
        selection = AmountState(networkFee = networkFee).resolve(
            draft = draft,
            options = bankWithdrawOptions(),
            isEditing = false,
        ),
        sheet = sheet,
        networkFee = networkFee,
        isBalanceHidden = false,
    )

    private companion object {
        val SPEI_DESTINATION = offrampDestination(
            rail = BankRail.SPEI,
            fee = BigDecimal("1"),
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
            minDays = 0,
            maxDays = 0,
        )
    }
}