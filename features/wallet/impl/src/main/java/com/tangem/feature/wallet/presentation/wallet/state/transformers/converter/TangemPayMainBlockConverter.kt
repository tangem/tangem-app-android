package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.isPlanTransitioningState
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.feature.wallet.child.wallet.model.intents.TangemPayIntents
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import java.util.Currency

internal class TangemPayMainBlockConverter(
    private val tangemPayClickIntents: TangemPayIntents,
    private val isAccountMultichainEnabled: Boolean,
) : Converter<AccountStatus.Payment, TangemPayMainUM> {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun convert(value: AccountStatus.Payment): TangemPayMainUM {
        return when (val statusValue = value.value) {
            is PaymentAccountStatusValue.Error.CardIssueFailed -> TangemPayMainUM.FailedToIssue(
                onClick = { tangemPayClickIntents.openDetails(value) },
            )
            is PaymentAccountStatusValue.Error.ExposedDevice -> TangemPayMainUM.ExposedDevice
            is PaymentAccountStatusValue.Error.NotSynced -> TangemPayMainUM.SyncNeeded
            is PaymentAccountStatusValue.Error.Unavailable -> TangemPayMainUM.TemporaryUnavailable
            is PaymentAccountStatusValue.IssuingCard -> TangemPayMainUM.IssuingCard(
                onClick = { tangemPayClickIntents.onIssuingCardClicked() },
            )
            is PaymentAccountStatusValue.AwaitingPlanSelection -> TangemPayMainUM.SelectPlan(
                onClick = { tangemPayClickIntents.onSelectPlanClicked(value) },
            )
            is PaymentAccountStatusValue.Inactive -> TangemPayMainUM.IssuingCard(
                onClick = { tangemPayClickIntents.openDetails(value) },
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
                balance = getBalanceText(statusValue.balance),
                balanceSubtitle = getBalanceSubtitle(statusValue.cryptoCurrency.symbol),
                shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                onClick = { tangemPayClickIntents.openDetails(value) },
            )
            is PaymentAccountStatusValue.Loaded -> {
                if (statusValue.tariffPlan?.isPlanTransitioningState == true) {
                    return TangemPayMainUM.IssuingCard(onClick = { tangemPayClickIntents.openDetails(value) })
                }
                val cardsCount = statusValue.cards.count()
                if (cardsCount == 0) return TangemPayMainUM.TemporaryUnavailable
                val subtitle = pluralReference(
                    id = R.plurals.tangempay_cards_count,
                    count = cardsCount,
                    formatArgs = wrappedList(cardsCount),
                )
                TangemPayMainUM.Content(
                    subtitle = subtitle,
                    isBalanceFlickering = statusValue.source == StatusSource.CACHE,
                    balance = getBalanceText(statusValue.balance),
                    balanceSubtitle = getBalanceSubtitle(statusValue.cryptoCurrency.symbol),
                    shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                    onClick = { tangemPayClickIntents.openDetails(value) },
                )
            }
        }
    }

    private fun getBalanceSubtitle(currencySymbol: String): TextReference {
        return stringReference(if (isAccountMultichainEnabled) USD_CURRENCY_CODE else currencySymbol)
    }

    private fun getBalanceText(balance: PaymentAccountStatusValue.Balance?): TextReference {
        if (balance == null) return stringReference(DASH_SIGN)
        val currency = Currency.getInstance(balance.fiatBalance.currency)
        return balance.fiatBalance.availableBalance.formatStyled {
            fiat(
                fiatCurrencyCode = currency.currencyCode,
                fiatCurrencySymbol = currency.symbol,
                spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
            )
        }
    }

    private companion object {
        const val USD_CURRENCY_CODE = "USD"
    }
}