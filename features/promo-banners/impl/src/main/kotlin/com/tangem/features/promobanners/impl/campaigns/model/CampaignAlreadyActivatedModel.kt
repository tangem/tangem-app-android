package com.tangem.features.promobanners.impl.campaigns.model

import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignAlreadyActivatedUM
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin model for the "campaign already activated" bottom sheet. The screen is read-only: it just renders
 * the token/account the cashback is paid out to.
 *
 * The state is the selection the user made in the activate flow, handed over via [Params] when enrollment
 * reports the campaign is already active. That selection is in-memory only (it carries non-serializable UI
 * state), so after process death it is `null` and the model assembles a placeholder state instead
 * (see [REDACTED_TASK_KEY]).
 */
@ModelScoped
internal class CampaignAlreadyActivatedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<Params>()
    private val accountIconConverter = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall)

    val uiState: StateFlow<CampaignAlreadyActivatedUM>
        field = MutableStateFlow(buildState())

    private fun buildState(): CampaignAlreadyActivatedUM {
        val selectedAccountUM = params.account?.let { account ->
            when (account) {
                is Account.CryptoPortfolio -> SelectedAccountUM(
                    iconState = accountIconConverter.convert(account),
                    name = account.accountName.toUM().value,
                )
                is Account.Payment -> SelectedAccountUM(
                    iconState = CurrencyIconState.PaymentAccount(size = AccountIconSize.ExtraSmall),
                    name = account.accountName.toUM().value,
                )
                is Account.Virtual -> null
            }
        }

        val tokenItem = TokenItemStateConverter(appCurrency = params.appCurrency).convert(params.currency)

        return CampaignAlreadyActivatedUM(
            selectedToken = tokenItem,
            selectedAccount = selectedAccountUM,
        )
    }

    data class Params(
        val campaignType: CampaignType,
        val account: Account?,
        val appCurrency: AppCurrency,
        val currency: CryptoCurrencyStatus,
        val onDismiss: () -> Unit,
    )
}