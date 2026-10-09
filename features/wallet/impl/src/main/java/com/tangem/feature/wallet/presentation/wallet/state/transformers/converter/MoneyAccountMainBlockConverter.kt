package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.MoneyAccountIntents
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.utils.converter.Converter

internal class MoneyAccountMainBlockConverter(
    private val moneyAccountIntents: MoneyAccountIntents,
) : Converter<AccountStatus.Money, MoneyAccountMainUM> {

    override fun convert(value: AccountStatus.Money): MoneyAccountMainUM {
        val userWalletId = value.account.userWalletId
        return when (val statusValue = value.value) {
            is MoneyAccountStatusValue.Empty,
            is MoneyAccountStatusValue.NotCreated,
            is MoneyAccountStatusValue.Error.ExposedDevice,
            -> MoneyAccountMainUM.Empty
            is MoneyAccountStatusValue.Loading -> MoneyAccountMainUM.Loading
            is MoneyAccountStatusValue.Onboarding -> statusValue.toUM(userWalletId)
            is MoneyAccountStatusValue.Account -> statusValue.toUM()
            is MoneyAccountStatusValue.Error.Unavailable,
            is MoneyAccountStatusValue.Error.NotSynced,
            -> MoneyAccountMainUM.Unavailable
        }
    }

    private fun MoneyAccountStatusValue.Onboarding.toUM(userWalletId: UserWalletId): MoneyAccountMainUM {
        return when (this) {
            is MoneyAccountStatusValue.Onboarding.Issuing,
            is MoneyAccountStatusValue.Onboarding.Activating,
            -> MoneyAccountMainUM.Onboarding(
                subtitle = resourceReference(R.string.money_account_main_status_activating),
                onClick = moneyAccountIntents::onMoneyAccountActivatingClicked,
            )
            is MoneyAccountStatusValue.Onboarding.KycRequired -> MoneyAccountMainUM.ActionRequired(
                balance = balance?.toUM(source),
                onClick = { moneyAccountIntents.onMoneyAccountKycRequiredClicked(userWalletId, customerId) },
            )
            is MoneyAccountStatusValue.Onboarding.UnderReview -> MoneyAccountMainUM.Onboarding(
                subtitle = resourceReference(R.string.money_account_main_status_verification),
                onClick = { moneyAccountIntents.onMoneyAccountVerificationClicked(userWalletId, customerId) },
            )
            is MoneyAccountStatusValue.Onboarding.VerificationDeclined -> MoneyAccountMainUM.ActionRequired(
                balance = balance?.toUM(source),
                onClick = { moneyAccountIntents.onMoneyAccountVerificationDeclinedClicked(userWalletId, customerId) },
            )
            is MoneyAccountStatusValue.Onboarding.CountryNotSupported -> MoneyAccountMainUM.ActionRequired(
                balance = balance?.toUM(source),
                onClick = { moneyAccountIntents.onMoneyAccountCountryNotSupportedClicked(userWalletId, countryCode) },
            )
        }
    }

    private fun MoneyAccountStatusValue.Account.toUM(): MoneyAccountMainUM {
        val balance = balance ?: return MoneyAccountMainUM.Loading

        return MoneyAccountMainUM.Content(
            subtitle = resourceReference(
                id = R.string.money_account_main_status_active,
                formatArgs = wrappedList(balance.cryptoCurrency.network.name),
            ),
            balance = balance.toUM(source),
            onClick = ::onAccountClick,
        )
    }

    private fun MoneyAccountStatusValue.Balance.toUM(source: StatusSource): MoneyAccountMainUM.BalanceUM {
        val currency = getJavaCurrencyByCode(currencyCode)
        return MoneyAccountMainUM.BalanceUM(
            amount = stringReference(
                cryptoAmount.format {
                    fiat(
                        fiatCurrencyCode = currency.currencyCode,
                        fiatCurrencySymbol = currency.symbol,
                    )
                },
            ),
            currency = stringReference(currencyCode),
            isFlickering = source == StatusSource.CACHE,
            isFromCache = source == StatusSource.ONLY_CACHE,
        )
    }

    private fun onAccountClick() {
        // TODO v_rodionov: #[REDACTED_TASK_KEY] navigate to MA screen
    }
}