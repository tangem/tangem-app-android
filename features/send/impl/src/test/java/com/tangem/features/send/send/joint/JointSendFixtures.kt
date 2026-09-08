package com.tangem.features.send.send.joint

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.math.BigInteger

internal const val FIXTURE_RECIPIENT = "0x1111111111111111111111111111111111111111"

internal val fixtureCoin: CryptoCurrency.Coin = MockCryptoCurrencyFactory().ethereum

internal fun createAmountData(crypto: BigDecimal, fiat: BigDecimal? = null): AmountState.Data = AmountState.Data(
    isPrimaryButtonEnabled = true,
    accountTitleUM = mockk(relaxed = true),
    availableBalanceCrypto = TextReference.EMPTY,
    availableBalanceFiat = TextReference.EMPTY,
    tokenName = TextReference.EMPTY,
    tokenIconState = mockk(relaxed = true),
    amountTextField = AmountFieldModel(
        value = crypto.toPlainString(),
        onValueChange = {},
        keyboardOptions = mockk(relaxed = true),
        keyboardActions = mockk(relaxed = true),
        cryptoAmount = Amount(currencySymbol = fixtureCoin.symbol, value = crypto, decimals = fixtureCoin.decimals),
        fiatAmount = Amount(currencySymbol = AppCurrency.Default.code, value = fiat, decimals = 2),
        isFiatValue = false,
        fiatValue = fiat?.toPlainString().orEmpty(),
        isFiatUnavailable = fiat == null,
        isValuePasted = false,
        onValuePastedTriggerDismiss = {},
        isError = false,
        isWarning = false,
        error = TextReference.EMPTY,
    ),
    appCurrency = AppCurrency.Default,
)

internal fun createDestinationContent(address: String = FIXTURE_RECIPIENT): DestinationUM.Content = DestinationUM.Content(
    isPrimaryButtonEnabled = true,
    addressTextField = DestinationTextFieldUM.RecipientAddress(
        value = address,
        keyboardOptions = mockk(relaxed = true),
        placeholder = TextReference.EMPTY,
        label = TextReference.EMPTY,
        isValuePasted = false,
    ),
    memoTextField = null,
    recent = persistentListOf(),
    wallets = persistentListOf(),
    networkName = fixtureCoin.network.name,
    isRecentHidden = true,
)

internal fun createSafeTx(
    safeTxGas: BigInteger = BigInteger.valueOf(150_000),
    baseGas: BigInteger = BigInteger.valueOf(110_000),
    gasPrice: BigInteger = BigInteger.valueOf(20_000_000_000),
): SafeTx = SafeTx(
    to = FIXTURE_RECIPIENT,
    value = BigInteger.ONE,
    data = "0x",
    operation = SafeTx.OPERATION_CALL,
    safeTxGas = safeTxGas,
    baseGas = baseGas,
    gasPrice = gasPrice,
    gasToken = "0x0000000000000000000000000000000000000000",
    refundReceiver = "0x2222222222222222222222222222222222222222",
    nonce = BigInteger.ZERO,
)