package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.feature.wallet.child.wallet.model.intents.TangemPayIntents
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.util.Currency

private const val POLYGON_CHAIN_ID = 137

internal class TangemPayMainBlockConverter(
    private val tangemPayClickIntents: TangemPayIntents,
    private val isRedesignEnabled: Boolean,
) : Converter<AccountStatus.Payment, TangemPayMainUM> {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun convert(value: AccountStatus.Payment): TangemPayMainUM {
        return when (val statusValue = value.value) {
            is PaymentAccountStatusValue.Error.CardIssueFailed -> TangemPayMainUM.FailedToIssue(
                onClick = { tangemPayClickIntents.onIssuingFailedClicked(statusValue.customerId) },
            )
            is PaymentAccountStatusValue.Error.ExposedDevice -> TangemPayMainUM.ExposedDevice
            is PaymentAccountStatusValue.Error.NotSynced -> TangemPayMainUM.SyncNeeded
            is PaymentAccountStatusValue.Error.Unavailable -> TangemPayMainUM.TemporaryUnavailable
            is PaymentAccountStatusValue.IssuingCard -> TangemPayMainUM.IssuingCard(
                onClick = { tangemPayClickIntents.onIssuingCardClicked() },
            )
            is PaymentAccountStatusValue.UnderReview -> TangemPayMainUM.UnderReview(
                subtitle = when (statusValue.kycStatus) {
                    KycStatus.REJECTED -> TextReference.Res(R.string.tangempay_kyc_has_failed)
                    else -> TextReference.Res(R.string.tangempay_kyc_in_progress)
                },
                onClick = {
                    when (statusValue.kycStatus) {
                        KycStatus.REJECTED -> tangemPayClickIntents.onKycRejectedClicked(
                            userWalletId = value.account.userWalletId,
                            customerId = statusValue.customerId,
                        )
                        else -> tangemPayClickIntents.onKycProgressClicked(value.account.userWalletId)
                    }
                },
            )
            is PaymentAccountStatusValue.Empty -> TangemPayMainUM.Empty
            is PaymentAccountStatusValue.NotCreated -> TangemPayMainUM.Empty
            is PaymentAccountStatusValue.Loading -> TangemPayMainUM.Loading
            is PaymentAccountStatusValue.Deactivated -> TangemPayMainUM.Content(
                subtitle = TextReference.Res(R.string.tangempay_status_deactivated),
                isBalanceFlickering = statusValue.source == StatusSource.CACHE,
                balance = getBalanceText(
                    currencyCode = statusValue.fiatBalance.currency,
                    balance = statusValue.fiatBalance.availableBalance,
                ),
                balanceSubtitle = stringReference("USDC"), // TODO hardcode for now
                shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                onClick = {
                    // Dummy config for deactivated account just to open details screen
                    tangemPayClickIntents.openDetails(
                        userWalletId = value.account.userWalletId,
                        config = TangemPayDetailsConfig(
                            customerId = "",
                            cardId = "",
                            isPinSet = false,
                            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
                            cardNumberEnd = "",
                            chainId = POLYGON_CHAIN_ID,
                            isTangemPayDeactivated = true,
                        ),
                    )
                },
            )
            is PaymentAccountStatusValue.Locked -> TangemPayMainUM.Content(
                subtitle = stringReference("*${statusValue.lastFourDigits}"),
                isBalanceFlickering = statusValue.source == StatusSource.CACHE,
                balance = getBalanceText(
                    currencyCode = statusValue.currencyCode,
                    balance = statusValue.fiatBalance.availableBalance,
                ),
                balanceSubtitle = stringReference("USDC"), // TODO hardcode for now
                shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                onClick = {
                    tangemPayClickIntents.openDetails(
                        value.account.userWalletId,
                        TangemPayDetailsConfig(
                            customerId = statusValue.customerId,
                            cardId = statusValue.cardId,
                            isPinSet = statusValue.isPinSet,
                            cardFrozenState = TangemPayCardFrozenState.Frozen,
                            cardNumberEnd = statusValue.lastFourDigits,
                            chainId = POLYGON_CHAIN_ID,
                            isTangemPayDeactivated = false,
                        ),
                    )
                },
            )
            is PaymentAccountStatusValue.Loaded -> TangemPayMainUM.Content(
                subtitle = stringReference("*${statusValue.lastFourDigits}"),
                isBalanceFlickering = statusValue.source == StatusSource.CACHE,
                balance = getBalanceText(
                    currencyCode = statusValue.currencyCode,
                    balance = statusValue.fiatBalance.availableBalance,
                ),
                balanceSubtitle = stringReference("USDC"), // TODO hardcode for now
                shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                onClick = {
                    tangemPayClickIntents.openDetails(
                        value.account.userWalletId,
                        TangemPayDetailsConfig(
                            customerId = statusValue.customerId,
                            cardId = statusValue.cardId,
                            isPinSet = statusValue.isPinSet,
                            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
                            cardNumberEnd = statusValue.lastFourDigits,
                            chainId = POLYGON_CHAIN_ID,
                            isTangemPayDeactivated = false,
                        ),
                    )
                },
            )
        }
    }

    private fun getBalanceText(currencyCode: String, balance: BigDecimal): TextReference {
        val currency = Currency.getInstance(currencyCode)
        val formattedBalance = if (isRedesignEnabled) {
            balance.formatStyled {
                fiat(
                    fiatCurrencyCode = currency.currencyCode,
                    fiatCurrencySymbol = currency.symbol,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                )
            }
        } else {
            stringReference(
                balance.format {
                    fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
                },
            )
        }
        return formattedBalance
    }
}