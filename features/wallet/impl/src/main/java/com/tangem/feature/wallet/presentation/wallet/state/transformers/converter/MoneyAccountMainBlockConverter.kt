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
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class MoneyAccountMainBlockConverter : Converter<AccountStatus.Virtual, MoneyAccountMainUM> {

    override fun convert(value: AccountStatus.Virtual): MoneyAccountMainUM {
        return when (val statusValue = value.value) {
            is VirtualAccountStatusValue.Empty,
            is VirtualAccountStatusValue.NotCreated,
            -> MoneyAccountMainUM.Empty
            is VirtualAccountStatusValue.Loading -> MoneyAccountMainUM.Loading
            is VirtualAccountStatusValue.Issuance -> statusValue.toUM()
            is VirtualAccountStatusValue.Issued -> statusValue.toUM()
            is VirtualAccountStatusValue.Error.Unavailable -> MoneyAccountMainUM.TemporaryUnavailable
            is VirtualAccountStatusValue.Error.NotSynced -> MoneyAccountMainUM.SyncNeeded
            is VirtualAccountStatusValue.Error.ExposedDevice -> MoneyAccountMainUM.ExposedDevice
        }
    }

    private fun VirtualAccountStatusValue.Issuance.toUM(): MoneyAccountMainUM {
        return when (this) {
            is VirtualAccountStatusValue.Issuance.UnderReview -> MoneyAccountMainUM.UnderReview(
                subtitle = TextReference.Res(R.string.tangempay_kyc_in_progress),
                onClick = ::onAccountClick,
            )
            is VirtualAccountStatusValue.Issuance.VerificationDeclined -> MoneyAccountMainUM.UnderReview(
                subtitle = TextReference.Res(R.string.tangempay_kyc_has_failed),
                onClick = ::onAccountClick,
            )
            is VirtualAccountStatusValue.Issuance.CountryNotSupported -> MoneyAccountMainUM.CountryNotSupported(
                onClick = ::onAccountClick,
            )
            is VirtualAccountStatusValue.Issuance.Issuing,
            is VirtualAccountStatusValue.Issuance.Activating,
            -> MoneyAccountMainUM.Provisioning(onClick = ::onAccountClick)
        }
    }

    private fun VirtualAccountStatusValue.Issued.toUM(): MoneyAccountMainUM {
        return when (this) {
            is VirtualAccountStatusValue.Issued.Deactivated -> MoneyAccountMainUM.Empty
            is VirtualAccountStatusValue.Issued.Active -> {
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