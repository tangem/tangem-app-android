package com.tangem.features.earn.impl.fake

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultAsset
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnTxRef
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

internal val FAKE_WALLET_BALANCE = BigDecimal("2.01")
internal val FAKE_POSITION = BigDecimal("0.5")
internal val FAKE_FEE_ETH = BigDecimal("0.00033")
internal val FAKE_FEE_GROWN_ETH = BigDecimal("0.00066")
internal val FAKE_QUOTE_TTL = 60.seconds
internal val FAKE_INSTANT = Instant.parse("2030-01-01T00:00:00Z")

private const val ETH_DECIMALS = 18
internal const val FAKE_ETH_CURRENCY_ID = "coin⟨ETH⟩ethereum"
private const val FAKE_WALLET_ID = "0123456789abcdef"
private const val FAKE_TX_REF_VERSION = 1

internal val FakeEarnSessionPreset.productId: String
    get() = when (this) {
        FakeEarnSessionPreset.Lending -> "lending"
        FakeEarnSessionPreset.Vault -> "vault"
    }

internal fun fakeFee(value: BigDecimal): Fee =
    Fee.Common(Amount(currencySymbol = "ETH", value = value, decimals = ETH_DECIMALS))

internal fun fakeTxAnalytics(actionId: String): EarnTxAnalytics = EarnTxAnalytics(params = mapOf("action" to actionId))

internal fun fakeTxRef(preset: FakeEarnSessionPreset, actionId: String): EarnTxRef = EarnTxRef(
    version = FAKE_TX_REF_VERSION,
    productId = EarnProductId(preset.productId),
    id = "fake-$actionId",
    userWalletId = UserWalletId(FAKE_WALLET_ID),
    currencyId = CryptoCurrency.ID.fromValue(FAKE_ETH_CURRENCY_ID),
    createdAt = FAKE_INSTANT,
    analytics = fakeTxAnalytics(actionId),
)

internal fun fakeResult(title: String, provider: String): EarnResult = EarnResult(
    title = stringReference(title),
    subtitle = stringReference("Your ETH in Main account is now earning in $provider"),
    chip = stringReference("Current 5.5% APY"),
    rows = emptyList(),
    primary = EarnResultButton(stringReference("Go to yield"), EarnFlowExit.ToYield),
    secondary = EarnResultButton(stringReference("Go to token"), EarnFlowExit.ToToken),
)

internal fun fakeWithdrawResult(title: String, period: String): EarnResult = EarnResult(
    title = stringReference(title),
    subtitle = null,
    chip = null,
    rows = listOf(
        EarnRow(label = stringReference("Withdrawal period"), value = EarnValue.Text(stringReference(period))),
        EarnRow(label = stringReference("Network fee"), value = EarnValue.Text(stringReference("$FAKE_FEE_ETH ETH"))),
    ),
    primary = EarnResultButton(stringReference("Done"), EarnFlowExit.Done),
    secondary = null,
    asset = EarnResultAsset.Status(TangemStatusIcon.Kind.Checkmark),
    isExplorerShown = true,
)