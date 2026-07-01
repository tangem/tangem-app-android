package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class VirtualAccountMainBlockConverter(
    private val isRedesignEnabled: Boolean,
) : Converter<AccountStatus.Virtual, VirtualAccountMainUM> {

    override fun convert(value: AccountStatus.Virtual): VirtualAccountMainUM {
        return when (val statusValue = value.value) {
            is VirtualAccountStatusValue.Empty -> VirtualAccountMainUM.Empty
            is VirtualAccountStatusValue.NotCreated -> VirtualAccountMainUM.Empty
            is VirtualAccountStatusValue.Loading -> VirtualAccountMainUM.Loading
            is VirtualAccountStatusValue.UnderReview -> VirtualAccountMainUM.UnderReview(
                subtitle = when (statusValue.kycStatus) {
                    KycStatus.REJECTED -> TextReference.Res(R.string.tangempay_kyc_has_failed)
                    else -> TextReference.Res(R.string.tangempay_kyc_in_progress)
                },
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is VirtualAccountStatusValue.Provisioning -> VirtualAccountMainUM.Provisioning(
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is VirtualAccountStatusValue.CountryNotSupported -> VirtualAccountMainUM.CountryNotSupported(
                onClick = {
                    // TODO([REDACTED_TASK_KEY]): navigate to VA screen
                },
            )
            is VirtualAccountStatusValue.Active -> VirtualAccountMainUM.Content(
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
            is VirtualAccountStatusValue.Error.Unavailable -> VirtualAccountMainUM.TemporaryUnavailable
            is VirtualAccountStatusValue.Error.NotSynced -> VirtualAccountMainUM.SyncNeeded
            is VirtualAccountStatusValue.Error.ExposedDevice -> VirtualAccountMainUM.ExposedDevice
        }
    }

    private fun getBalanceText(currencyCode: String, balance: BigDecimal): TextReference {
        val currency = getJavaCurrencyByCode(currencyCode)
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