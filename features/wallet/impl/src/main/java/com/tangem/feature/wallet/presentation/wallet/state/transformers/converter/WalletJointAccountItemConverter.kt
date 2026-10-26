package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.common.ui.account.toAvatarState
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.actionmessage.TangemActionMessage
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.ds2.util.TangemPriceChange.Direction.Neutral
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.core.ui.res.generated.icons.ic_user_plus_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import java.math.BigDecimal

/** A joint account that is not active yet, as one [TangemAccountRow] of the wallet account list */
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
            is JointAccountStatusValue.Loading -> TangemAccountRow.State.Shimmer(id = account.accountId.value)
            is JointAccountStatusValue.Error.Unavailable -> TangemAccountRow.State.BalanceError(
                id = account.accountId.value,
                title = account.accountName.toUM().value,
            )
            is JointAccountStatusValue.Error.Compromised -> compromised(account = account, status = status)
            is JointAccountStatusValue.Pending -> notActive(
                account = account,
                totalMembers = status.totalMembers,
                actionMessage = pendingMessage(
                    account = account,
                    freeSlots = status.totalMembers - status.joinedMembers,
                ),
            )
            is JointAccountStatusValue.Confirming -> notActive(
                account = account,
                totalMembers = status.totalMembers,
                actionMessage = confirmingMessage(account = account, status = status),
            )
            is JointAccountStatusValue.Active -> error("An active joint account is a regular portfolio row")
        }

        return TokensListItemUM2.JointPending(state = state, avatar = account.toAvatarState())
    }

    private fun notActive(
        account: Account.Joint,
        totalMembers: Int,
        actionMessage: TangemAccountRow.ActionMessage?,
    ): TangemAccountRow.State.NotActive {
        return TangemAccountRow.State.NotActive(
            id = account.accountId.value,
            title = account.accountName.toUM().value,
            description = membersCount(totalMembers),
            balance = stringReference(zeroBalanceText()),
            priceChange = TangemPriceChange.State(value = zeroPriceChange(), direction = Neutral),
            actionMessage = actionMessage,
            onClick = { onAccountClick(userWalletId, account.accountId.value) },
        )
    }

    /** The composition does not verify: the badge takes the place of the balance, the tap explains why */
    private fun compromised(
        account: Account.Joint,
        status: JointAccountStatusValue.Error.Compromised,
    ): TangemAccountRow.State.Unreachable {
        return TangemAccountRow.State.Unreachable(
            id = account.accountId.value,
            title = account.accountName.toUM().value,
            badge = resourceReference(R.string.joint_account_main_cant_verify),
            label = pluralReference(
                id = R.plurals.common_tokens_count,
                count = status.tokensCount,
                formatArgs = wrappedList(status.tokensCount),
            ),
            description = membersCount(status.totalMembers),
            onClick = { onCompromisedAccountClick(userWalletId) },
        )
    }

    /** Only the creator holds the invites, so their presence on this device marks the creator's row */
    private fun Account.Joint.belongsToCreator(): Boolean = accountId.value in accountIdsWithInvites

    private fun membersCount(totalMembers: Int): TextReference = pluralReference(
        id = R.plurals.joint_account_members_count,
        count = totalMembers,
        formatArgs = wrappedList(totalMembers),
    )

    private fun pendingMessage(account: Account.Joint, freeSlots: Int): TangemAccountRow.ActionMessage? {
        if (freeSlots <= 0) return null

        return if (account.belongsToCreator()) {
            actionMessage(
                account = account,
                text = resourceReference(id = R.string.joint_account_invite_members_to_join),
                icon = Icons.ic_user_plus_16,
            )
        } else {
            actionMessage(
                account = account,
                text = pluralReference(
                    id = R.plurals.joint_account_main_waiting_members,
                    count = freeSlots,
                    formatArgs = wrappedList(freeSlots),
                ),
                icon = Icons.ic_clock_16,
            )
        }
    }

    private fun confirmingMessage(
        account: Account.Joint,
        status: JointAccountStatusValue.Confirming,
    ): TangemAccountRow.ActionMessage {
        if (!status.isOwnVerified) {
            return actionMessage(
                account = account,
                text = resourceReference(id = R.string.joint_account_confirm_shared_address),
                icon = Icons.ic_checkmark_16,
            )
        }

        val notConfirmed = status.totalMembers - status.verifiedMembers
        return actionMessage(
            account = account,
            text = pluralReference(
                id = R.plurals.joint_account_main_waiting_confirmations,
                count = notConfirmed,
                formatArgs = wrappedList(notConfirmed),
            ),
            icon = Icons.ic_clock_16,
        )
    }

    private fun actionMessage(account: Account.Joint, text: TextReference, icon: ImageVector) =
        TangemAccountRow.ActionMessage(
            text = text,
            icon = icon,
            onClick = { onAccountClick(userWalletId, account.accountId.value) },
            variant = TangemActionMessage.Variant.Warning,
        )

    private fun zeroBalanceText(): String {
        return BigDecimal.ZERO.format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
    }

    private fun zeroPriceChange(): TextReference = stringReference(BigDecimal.ZERO.format { percent() })
}