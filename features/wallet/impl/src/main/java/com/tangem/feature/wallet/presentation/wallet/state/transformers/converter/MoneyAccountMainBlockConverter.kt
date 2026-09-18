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
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class MoneyAccountMainBlockConverter : Converter<AccountStatus.Money, MoneyAccountMainUM> {

    override fun convert(value: AccountStatus.Money): MoneyAccountMainUM {
        return when (val statusValue = value.value) {
            is MoneyAccountStatusValue.Empty,
            is MoneyAccountStatusValue.NotCreated,
            -> MoneyAccountMainUM.Empty
            is MoneyAccountStatusValue.Loading -> MoneyAccountMainUM.Loading
            is MoneyAccountStatusValue.Onboarding -> statusValue.toUM()
            is MoneyAccountStatusValue.Account -> statusValue.toUM()
            is MoneyAccountStatusValue.Error.Unavailable -> MoneyAccountMainUM.TemporaryUnavailable
            is MoneyAccountStatusValue.Error.NotSynced -> MoneyAccountMainUM.SyncNeeded
            is MoneyAccountStatusValue.Error.ExposedDevice -> MoneyAccountMainUM.ExposedDevice
        }
    }

    private fun MoneyAccountStatusValue.Onboarding.toUM(): MoneyAccountMainUM {
        return when (this) {
            is MoneyAccountStatusValue.Onboarding.UnderReview -> MoneyAccountMainUM.UnderReview(
                subtitle = TextReference.Res(R.string.tangempay_kyc_in_progress),
                onClick = ::onAccountClick,
            )
            is MoneyAccountStatusValue.Onboarding.VerificationDeclined -> MoneyAccountMainUM.UnderReview(
                subtitle = TextReference.Res(R.string.tangempay_kyc_has_failed),
                onClick = ::onAccountClick,
            )
            is MoneyAccountStatusValue.Onboarding.CountryNotSupported -> MoneyAccountMainUM.CountryNotSupported(
                onClick = ::onAccountClick,
            )
            is MoneyAccountStatusValue.Onboarding.Issuing,
            is MoneyAccountStatusValue.Onboarding.Activating,
            -> MoneyAccountMainUM.Provisioning(onClick = ::onAccountClick)
        }
    }

    private fun MoneyAccountStatusValue.Account.toUM(): MoneyAccountMainUM {
        return when (this) {
            is MoneyAccountStatusValue.Account.Deactivated -> MoneyAccountMainUM.Empty
            is MoneyAccountStatusValue.Account.Active -> {
                val balance = balance ?: return MoneyAccountMainUM.Loading

                MoneyAccountMainUM.Content(
                    subtitle = stringReference(balance.cryptoCurrency.symbol),
                    isBalanceFlickering = source == StatusSource.CACHE,
                    balance = getBalanceText(currencyCode = balance.currencyCode, balance = balance.cryptoAmount),
                    balanceSubtitle = stringReference(balance.cryptoCurrency.symbol),
                    shouldShowOnlyCacheWarning = source == StatusSource.ONLY_CACHE,
                    onClick = ::onAccountClick,
                )
            }
        }
    }

    private fun onAccountClick() {
        // TODO([REDACTED_TASK_KEY]): navigate to VA screen
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