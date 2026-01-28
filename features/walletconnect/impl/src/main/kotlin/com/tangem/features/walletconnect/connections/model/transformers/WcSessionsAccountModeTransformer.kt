package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.features.walletconnect.connections.entity.WcConnectedAppInfo
import com.tangem.features.walletconnect.connections.entity.WcConnections
import com.tangem.features.walletconnect.connections.entity.WcConnectionsItem
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class WcSessionsAccountModeTransformer(
    private val sessionsMap: Map<UserWallet, List<WcSession>>,
    private val accountList: List<AccountList>,
    private val openAppInfoModal: (WcSession) -> Unit,
) : Transformer<WcConnectionsState> {

    override fun transform(prevState: WcConnectionsState): WcConnectionsState {
        val items = mutableListOf<WcConnectionsItem>()
        val lockedWallet = mutableListOf<WcConnectionsItem.WalletHeader>()

        accountList.forEach { accountList ->
            val userWallet = sessionsMap.keys.find { it.walletId == accountList.userWalletId }
                ?: return@forEach
            val sessions = sessionsMap[userWallet] ?: return@forEach

            if (userWallet.isLocked) {
                val lockedItem = WcConnectionsItem.WalletHeader(
                    id = userWallet.walletId.stringValue,
                    walletName = userWallet.name,
                    isLocked = true,
                )
                lockedWallet.add(lockedItem)
                return@forEach
            }

            val walletHeader = WcConnectionsItem.WalletHeader(
                id = userWallet.walletId.stringValue,
                walletName = userWallet.name,
                isLocked = false,
            )
            items.add(walletHeader)

            accountList.accounts.forEach accountsForEach@{ account ->
                val accountSessions = sessions.filter { it.account?.accountId == account.accountId }
                if (accountSessions.isEmpty()) return@accountsForEach
                val connectedApps = accountSessions.map { dappSession ->
                    with(dappSession.sdkModel) {
                        WcConnectedAppInfo(
                            name = appMetaData.name,
                            iconUrl = appMetaData.icons.firstOrNull().orEmpty(),
                            subtitle = WcAppSubtitleConverter.convert(appMetaData),
                            verifiedState = WcDAppVerifiedStateConverter {}
                                .convert(dappSession.securityStatus to appMetaData.name),
                            onClick = { openAppInfoModal(dappSession) },
                        )
                    }
                }
                val accountIcon = when (account) {
                    is Account.CryptoPortfolio -> account.icon
                    is Account.Payment -> TODO("[REDACTED_JIRA]")
                }

                val accountTitle = AccountTitleUM.Account(
                    prefixText = TextReference.EMPTY,
                    name = account.accountName.toUM().value,
                    icon = accountIcon.toUM(),
                )

                val accountConnections = WcConnectionsItem.PortfolioConnections(
                    id = userWallet.walletId.stringValue + account.accountId.value,
                    portfolioTitle = accountTitle,
                    connectedApps = connectedApps.toPersistentList(),
                )
                items.add(accountConnections)
            }
        }
        return WcConnectionsState(
            topAppBarConfig = prevState.topAppBarConfig,
            connections = WcConnections.AccountMode(items.plus(lockedWallet).toPersistentList()),
            onNewConnectionClick = prevState.onNewConnectionClick,
        )
    }
}