package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class MoneyAccountMainBlockConverter : Converter<AccountStatus.Money, MoneyAccountMainUM> {

    override fun convert(value: AccountStatus.Money): MoneyAccountMainUM {
        return when (val statusValue = value.value) {
            is MoneyAccountStatusValue.Empty -> MoneyAccountMainUM.Empty
            is MoneyAccountStatusValue.NotCreated -> MoneyAccountMainUM.Empty
            is MoneyAccountStatusValue.Loading -> MoneyAccountMainUM.Loading
            is MoneyAccountStatusValue.UnderReview -> MoneyAccountMainUM.UnderReview(
                subtitle = when (statusValue.kycStatus) {
                    KycStatus.REJECTED -> TextReference.Res(R.string.tangempay_kyc_has_failed)
                    else -> TextReference.Res(R.string.tangempay_kyc_in_progress)
                },
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is MoneyAccountStatusValue.Provisioning -> MoneyAccountMainUM.Provisioning(
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is MoneyAccountStatusValue.CountryNotSupported -> MoneyAccountMainUM.CountryNotSupported(
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is MoneyAccountStatusValue.Active -> MoneyAccountMainUM.Content(
                subtitle = stringReference(statusValue.cryptoCurrency.symbol),
                isBalanceFlickering = statusValue.source == StatusSource.CACHE,
                balance = getBalanceText(
                    currencyCode = statusValue.fiatBalance.currency,
                    balance = statusValue.fiatBalance.availableBalance,
                ),
                balanceSubtitle = stringReference(statusValue.cryptoCurrency.symbol),
                shouldShowOnlyCacheWarning = statusValue.source == StatusSource.ONLY_CACHE,
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is MoneyAccountStatusValue.Error.Unavailable -> MoneyAccountMainUM.TemporaryUnavailable
            is MoneyAccountStatusValue.Error.NotSynced -> MoneyAccountMainUM.SyncNeeded
            is MoneyAccountStatusValue.Error.ExposedDevice -> MoneyAccountMainUM.ExposedDevice
        }
    }

    private fun getBalanceText(currencyCode: String, balance: BigDecimal): TextReference {
        val currency = getJavaCurrencyByCode(currencyCode)
        return balance.formatStyled {
            fiat(
                fiatCurrencyCode = currency.currencyCode,
                fiatCurrencySymbol = currency.symbol,
                spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
            )
        }
    }
}