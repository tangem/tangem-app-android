package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.ds2.util.TangemPriceChange.Direction.Neutral
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.core.ui.res.generated.icons.ic_user_plus_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.features.jointaccount.main.JointAccountMainUM
import com.tangem.utils.StringsSigns
import java.math.BigDecimal

internal class WalletJointAccountItemConverter(
    private val appCurrency: AppCurrency,
    private val userWalletId: UserWalletId,
    private val accountIdsWithInvites: Set<String>,
    private val onAccountClick: (UserWalletId, String) -> Unit,
    private val onCompromisedAccountClick: (UserWalletId) -> Unit,
) {

    fun convert(value: AccountStatus.Joint): TokensListItemUM2.JointPending {
        val account = value.account
        val state = when (val status = value.value) {
            is JointAccountStatusValue.Loading -> JointAccountMainUM.Loading
            is JointAccountStatusValue.Error.Unavailable -> JointAccountMainUM.TemporaryUnavailable
            is JointAccountStatusValue.Error.Compromised -> compromised(value = value, status = status)
            is JointAccountStatusValue.Pending -> waitingMembers(
                account = account,
                subtitle = resourceReference(
                    id = R.string.joint_account_members_progress,
                    formatArgs = wrappedList(status.joinedMembers, status.totalMembers),
                ),
                banner = inviteBanner(account = account, hasFreeSlots = status.joinedMembers < status.totalMembers),
            )
            is JointAccountStatusValue.Confirming -> waitingMembers(
                account = account,
                subtitle = resourceReference(id = R.string.joint_account_waiting_activation),
                banner = activateBanner(account = account),
            )
            is JointAccountStatusValue.Active -> error("An active joint account is a regular portfolio row")
        }

        return TokensListItemUM2.JointPending(accountId = account.accountId.value, state = state)
    }

    private fun waitingMembers(
        account: Account.Joint,
        subtitle: TextReference,
        banner: JointAccountMainUM.BannerUM?,
    ): JointAccountMainUM.WaitingMembers {
        return JointAccountMainUM.WaitingMembers(
            title = account.accountName.toUM().value,
            subtitle = subtitle,
            icon = CryptoPortfolioIconConverter.convert(account.icon),
            balance = stringReference(zeroBalanceText()),
            priceChange = TangemPriceChange.State(value = zeroPriceChange(), direction = Neutral),
            banner = banner,
            onClick = { onAccountClick(userWalletId, account.accountId.value) },
        )
    }

    private fun compromised(
        value: AccountStatus.Joint,
        status: JointAccountStatusValue.Error.Compromised,
    ): JointAccountMainUM.Blocked {
        val account = value.account
        val balance = (status.fiatBalance as? TotalFiatBalance.Loaded)?.amount?.format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }

        return JointAccountMainUM.Blocked(
            title = account.accountName.toUM().value,
            subtitle = combinedReference(
                pluralReference(
                    id = R.plurals.common_tokens_count,
                    count = status.tokensCount,
                    formatArgs = wrappedList(status.tokensCount),
                ),
                stringReference(" ${StringsSigns.DOT} "),
                pluralReference(
                    id = R.plurals.joint_account_members_count,
                    count = status.totalMembers,
                    formatArgs = wrappedList(status.totalMembers),
                ),
            ),
            icon = CryptoPortfolioIconConverter.convert(account.icon),
            balance = stringReference(balance ?: StringsSigns.DASH_SIGN),
            priceChange = value.priceChangeLce.getOrNull()?.toState(),
            onClick = { onCompromisedAccountClick(userWalletId) },
        )
    }

    private fun PriceChange.toState(): TangemPriceChange.State = TangemPriceChange.State(
        value = stringReference(value.abs().format { percent() }),
        direction = when (value.signum()) {
            1 -> TangemPriceChange.Direction.Up
            -1 -> TangemPriceChange.Direction.Down
            else -> Neutral
        },
    )

    /** Only the creator holds the invites, so their presence on this device marks the creator's row */
    private fun Account.Joint.belongsToCreator(): Boolean = accountId.value in accountIdsWithInvites

    private fun inviteBanner(account: Account.Joint, hasFreeSlots: Boolean): JointAccountMainUM.BannerUM? {
        if (!account.belongsToCreator() || !hasFreeSlots) return null

        return JointAccountMainUM.BannerUM(
            text = resourceReference(id = R.string.joint_account_invite_members_to_join),
            icon = Icons.ic_user_plus_16,
            onClick = { onAccountClick(userWalletId, account.accountId.value) },
        )
    }

    private fun activateBanner(account: Account.Joint): JointAccountMainUM.BannerUM? {
        if (!account.belongsToCreator()) return null

        return JointAccountMainUM.BannerUM(
            text = resourceReference(id = R.string.joint_account_activation_account_button_title),
            icon = Icons.ic_clock_16,
            onClick = { onAccountClick(userWalletId, account.accountId.value) },
        )
    }

    private fun zeroBalanceText(): String {
        return BigDecimal.ZERO.format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
    }

    private fun zeroPriceChange(): TextReference = stringReference(BigDecimal.ZERO.format { percent() })
}