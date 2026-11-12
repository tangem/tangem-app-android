package com.tangem.features.earn.flow.impl.model

import io.mockk.mockk
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.session.EarnActionInput
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.VerifiedPlan
import com.tangem.features.earn.product.api.session.WithdrawMode
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.test.mock.yield.MockYieldSupply
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import kotlin.time.Duration

internal val testEthereum = MockYieldSupply.coin()

internal val testUserWalletId = UserWalletId("0123456789abcdef")

internal val TEST_BALANCE = BigDecimal("2.01")

internal fun testStatus(
    amount: BigDecimal = TEST_BALANCE,
    fiatRate: BigDecimal = BigDecimal("4000"),
): CryptoCurrencyStatus = MockYieldSupply.loadedStatus(
    currency = testEthereum,
    amount = amount,
    fiatAmount = amount * fiatRate,
    fiatRate = fiatRate,
)

internal fun testFee(value: String): Fee =
    Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18))

internal fun testFeeContent(
    value: String,
    isPrimaryButtonEnabled: Boolean = true,
    extended: TransactionFeeExtended? = null,
): FeeSelectorUM.Content {
    val fee = testFee(value)
    return FeeSelectorUM.Content(
        isPrimaryButtonEnabled = isPrimaryButtonEnabled,
        fees = TransactionFee.Single(fee),
        feeItems = persistentListOf(FeeItem.Market(fee)),
        selectedFeeItem = FeeItem.Market(fee),
        feeExtraInfo = FeeExtraInfo(
            isFeeApproximate = false,
            isFeeConvertibleToFiat = true,
            isTronToken = false,
            feeCryptoCurrencyStatus = testStatus(),
            transactionFeeExtended = extended,
        ),
        feeFiatRateUM = null,
        feeNonce = FeeNonce.None,
    )
}

internal val testFlowProduct = EarnFlowProduct(
    productId = EarnProductId("lending"),
    title = stringReference("Lending on Aave"),
    provider = EarnProvider(title = stringReference("Aave"), icon = ImageReference.Res(0)),
    analytics = EarnProductAnalytics(category = "Earn", product = "Lending"),
)

internal fun testParams(action: EarnFlowAction, callback: EarnFlowComponent.ModelCallback) = EarnFlowComponent.Params(
    product = EarnProduct.Params(userWalletId = testUserWalletId, currency = testEthereum),
    flowProduct = testFlowProduct,
    action = action,
    source = EarnEntrySource.TokenPage,
    callback = callback,
)

internal object TestClaim : EarnFlowAction {
    override val id: String = "claim"
    override val label: TextReference = stringReference("Claim")
    override val input: EarnActionInput = EarnActionInput.None
    override val isGaslessAllowed: Boolean = false
    override fun title(productTitle: TextReference): TextReference = productTitle
}

internal data class TestPlan(
    override val fee: Fee = testFee("0.00033"),
    override val expectedAmount: BigDecimal? = BigDecimal("0.5"),
    override val rows: List<EarnRow> = emptyList(),
    override val withdrawMode: WithdrawMode? = null,
    override val quoteTtl: Duration? = null,
    override val notice: TextReference? = null,
    override val isEstimate: Boolean = false,
) : EarnPlan

internal data class TestVerifiedPlan(override val plan: EarnPlan) : VerifiedPlan

internal fun testResult(
    primary: EarnResultButton? = EarnResultButton(stringReference("Go to yield"), EarnFlowExit.ToYield),
    secondary: EarnResultButton? = null,
) = EarnResult(
    title = stringReference("Deposited 0.5 ETH"),
    subtitle = null,
    chip = null,
    rows = emptyList(),
    primary = primary,
    secondary = secondary,
)

/** An entered 0.5 ETH: the shared transformers mark it valid. */
internal fun validAmountState(): AmountState {
    val status = testStatus()
    val boundary = EnterAmountBoundary(amount = BigDecimal("0.5"), fiatRate = status.value.fiatRate)
    val initial = AmountStateConverter(
        clickIntents = mockk(relaxed = true),
        appCurrency = AppCurrency.Default,
        cryptoCurrencyStatus = status,
        maxEnterAmount = EnterAmountBoundary(amount = TEST_BALANCE, fiatRate = status.value.fiatRate),
        iconStateConverter = CryptoCurrencyToIconStateConverter(),
        isBalanceHidden = false,
        accountTitleUM = AccountTitleUM.Text(TextReference.EMPTY),
    ).convert(AmountParameters(title = TextReference.EMPTY, value = ""))
    return AmountFieldSetMaxAmountTransformer(status, boundary, minAmount = null).transform(initial)
}