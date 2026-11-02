package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.features.walletconnect.connections.entity.WcConnectedAppInfo
import com.tangem.features.walletconnect.connections.entity.WcDAppGroupUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Builds the dApp part of one wallet, as one group per account or a single untitled group when
 * accounts are off.
 */
internal object WcDAppGroupsConverter : Converter<WcDAppGroupsConverter.Input, ImmutableList<WcDAppGroupUM>> {

    override fun convert(value: Input): ImmutableList<WcDAppGroupUM> = when {
        value.sessions.isEmpty() -> persistentListOf()
        value.isAccountMode -> value.createAccountGroups()
        else -> value.createWalletGroup()
    }

    private fun Input.createWalletGroup() = persistentListOf(
        WcDAppGroupUM(
            id = wallet.walletId.stringValue,
            title = null,
            connectedApps = sessions.map { createAppInfo(it) }.toPersistentList(),
        ),
    )

    private fun Input.createAccountGroups(): ImmutableList<WcDAppGroupUM> {
        val accounts = accountList?.accounts?.filterIsInstance<Account.CryptoPortfolio>().orEmpty()

        return accounts.mapNotNull { account ->
            val accountSessions = sessions.filter { it.account.accountId == account.accountId }
            if (accountSessions.isEmpty()) return@mapNotNull null

            WcDAppGroupUM(
                id = wallet.walletId.stringValue + account.accountId.value,
                title = AccountTitleUM.Account(
                    prefixText = TextReference.EMPTY,
                    name = account.accountName.toUM().value,
                    icon = CryptoPortfolioIconConverter.convert(account.icon),
                ),
                connectedApps = accountSessions.map { createAppInfo(it) }.toPersistentList(),
            )
        }.toPersistentList()
    }

    private fun Input.createAppInfo(session: WcSession) = with(session.sdkModel) {
        WcConnectedAppInfo(
            name = appMetaData.name,
            iconUrl = appMetaData.icons.firstOrNull().orEmpty(),
            subtitle = WcAppSubtitleConverter.convert(appMetaData),
            verifiedState = WcDAppVerifiedStateConverter {}.convert(session.securityStatus to appMetaData.name),
            onClick = { onAppClick(session) },
        )
    }

    data class Input(
        val wallet: UserWallet,
        val sessions: List<WcSession>,
        val accountList: AccountList?,
        val isAccountMode: Boolean,
        val onAppClick: (WcSession) -> Unit,
    )
}