package com.tangem.features.tangempay.withdraw.hub.state

import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.recents.RecentTokenTransfer
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.hub.model.OptionsState
import com.tangem.features.tangempay.withdraw.hub.model.RecentsState
import com.tangem.features.tangempay.withdraw.hub.model.TokenRecentsState
import com.tangem.features.tangempay.withdraw.hub.model.WithdrawHubState
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.ActionCardUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.BankSectionUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentRecipientUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentTokenUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.TokenRecentsUM
import com.tangem.features.tangempay.withdraw.toShortName
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayWithdrawHubUMFactory(
    private val sourceAccount: BankWithdrawSourceAccount,
    private val callbacks: Callbacks,
) {

    // The payment account's networks are dictated by the backend environment, so a testnet one must not grey out.
    private val iconConverter = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false)

    fun create(state: WithdrawHubState): TangemPayWithdrawHubUM = when (sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> TangemPayWithdrawHubUM(
            title = resourceReference(R.string.tangempay_card_details_withdraw),
            swapSection = createSwapSection(
                subtitle = resourceReference(R.string.tangempay_withdraw_hub_swap_section_subtitle),
                tokenRecents = state.tokenRecents,
            ),
            bankSection = createBankSection(
                state = state,
                unavailableSubtitle = resourceReference(
                    R.string.tangempay_withdraw_hub_bank_section_subtitle_unavailable,
                ),
            ),
            onBackClick = callbacks.onBackClick,
        )
        is BankWithdrawSourceAccount.Money -> TangemPayWithdrawHubUM(
            title = resourceReference(R.string.common_send),
            swapSection = createSwapSection(subtitle = null, tokenRecents = state.tokenRecents),
            bankSection = createBankSection(
                state = state,
                unavailableSubtitle = resourceReference(R.string.money_account_send_bank_transfer_unavailable),
            ),
            onBackClick = callbacks.onBackClick,
        )
    }

    private fun createSwapSection(subtitle: TextReference?, tokenRecents: TokenRecentsState) =
        TangemPayWithdrawHubUM.SwapSectionUM(
            subtitle = subtitle,
            withinPortfolio = ActionCardUM(
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_swap_horizontal_20,
                    tintReference = { TangemTheme.colors3.icon.staticDark },
                ),
                title = resourceReference(R.string.tangempay_withdraw_hub_within_portfolio),
                onClick = callbacks.onWithinPortfolioClick,
            ),
            recents = createTokenRecents(tokenRecents),
        )

    private fun createBankSection(state: WithdrawHubState, unavailableSubtitle: TextReference): BankSectionUM? =
        when (state.options) {
            OptionsState.NotOffered -> null
            OptionsState.Loading -> BankSectionUM.Loading
            OptionsState.Unavailable,
            is OptionsState.Ready,
            -> BankSectionUM.Content(
                toPersonalAccount = ActionCardUM(
                    icon = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_user_filled_16,
                        tintReference = { TangemTheme.colors3.icon.staticDark },
                    ),
                    title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
                    onClick = callbacks.onToPersonalAccountClick,
                ),
                toBusinessAccount = ActionCardUM(
                    icon = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_briefcase_filled_16,
                        tintReference = { TangemTheme.colors3.icon.staticDark },
                    ),
                    title = resourceReference(R.string.tangempay_withdraw_hub_to_business_account),
                    onClick = callbacks.onToBusinessAccountClick,
                ),
                recents = createRecents(state.recents),
                isAvailable = state.options is OptionsState.Ready,
                unavailableSubtitle = unavailableSubtitle,
            )
        }

    private fun createRecents(state: RecentsState): RecentsUM = when (state) {
        RecentsState.Loading -> RecentsUM.Loading
        RecentsState.Failed -> RecentsUM.Error(onRetryClick = callbacks.onRetryRecentsClick)
        is RecentsState.Loaded -> if (state.accounts.isEmpty()) {
            RecentsUM.Empty
        } else {
            RecentsUM.Content(
                recipients = state.accounts.map(::toRecipient).toImmutableList(),
                onRecipientClick = callbacks.onRecentRecipientClick,
                onSeeAllClick = callbacks.onSeeAllClick.takeIf { state.hasMoreAccounts },
            )
        }
    }

    private fun toRecipient(account: BankAccount): RecentRecipientUM {
        val name = account.details.beneficiary.toShortName()
        return RecentRecipientUM(
            id = account.id,
            initial = name.take(1).uppercase(),
            name = name,
            accountNumber = account.details.accountNumber,
        )
    }

    private fun createTokenRecents(state: TokenRecentsState): TokenRecentsUM = when (state) {
        TokenRecentsState.Loading -> TokenRecentsUM.Loading
        is TokenRecentsState.Loaded -> if (state.transfers.isEmpty()) {
            TokenRecentsUM.Empty
        } else {
            TokenRecentsUM.Content(
                tokens = state.transfers.map(::toRecentToken).toImmutableList(),
                onTokenClick = callbacks.onRecentTokenClick,
            )
        }
    }

    private fun toRecentToken(transfer: RecentTokenTransfer): RecentTokenUM = RecentTokenUM(
        id = transfer.currency.id.value,
        icon = iconConverter.convert(transfer.currency),
        name = transfer.currency.name,
        accountName = transfer.account.accountName.toUM().value,
        accountIcon = CryptoPortfolioIconConverter.convert(transfer.account.icon),
    )

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onWithinPortfolioClick: () -> Unit,
        val onRecentTokenClick: (currencyId: String) -> Unit,
        val onToPersonalAccountClick: () -> Unit,
        val onToBusinessAccountClick: () -> Unit,
        val onRecentRecipientClick: (bankAccountId: String) -> Unit,
        val onSeeAllClick: () -> Unit,
        val onRetryRecentsClick: () -> Unit,
    )
}