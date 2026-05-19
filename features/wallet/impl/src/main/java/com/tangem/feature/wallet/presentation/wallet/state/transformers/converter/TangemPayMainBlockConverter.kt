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
import com.tangem.feature.wallet.child.wallet.model.intents.TangemPayIntents
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

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
                    fiatBalance = statusValue.fiatBalance,
                ),
                balanceSubtitle = stringReference(statusValue.cryptoCurrency.symbol),
                shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                onClick = { tangemPayClickIntents.openDetails(value) },
            )
            is PaymentAccountStatusValue.Loaded -> {
                val card = statusValue.cards.firstOrNull() ?: return TangemPayMainUM.TemporaryUnavailable
                TangemPayMainUM.Content(
                    subtitle = stringReference("*${card.lastDigits}"),
                    isBalanceFlickering = statusValue.source == StatusSource.CACHE,
                    balance = getBalanceText(
                        fiatBalance = statusValue.fiatBalance,
                    ),
                    balanceSubtitle = stringReference(statusValue.cryptoCurrency.symbol),
                    shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                    onClick = { tangemPayClickIntents.openDetails(value) },
                )
            }
        }
    }

    private fun getBalanceText(fiatBalance: PaymentAccountStatusValue.FiatBalance): TextReference {
        return getBalanceText(
            currencyCode = fiatBalance.currencyCode,
            currencySymbol = fiatBalance.symbol,
            balance = fiatBalance.availableBalance,
        )
    }

    private fun getBalanceText(currencyCode: String, currencySymbol: String, balance: BigDecimal): TextReference {
        val formattedBalance = if (isRedesignEnabled) {
            balance.formatStyled {
                fiat(
                    fiatCurrencyCode = currencyCode,
                    fiatCurrencySymbol = currencySymbol,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                )
            }
        } else {
            stringReference(
                balance.format {
                    fiat(fiatCurrencyCode = currencyCode, fiatCurrencySymbol = currencySymbol)
                },
            )
        }
        return formattedBalance
    }
}