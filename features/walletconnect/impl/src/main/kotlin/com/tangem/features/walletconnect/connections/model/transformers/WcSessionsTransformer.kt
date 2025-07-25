package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.walletconnect.connections.entity.WcConnectedAppInfo
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.features.walletconnect.connections.entity.WcConnectionsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class WcSessionsTransformer(
    private val sessionsMap: Map<UserWallet, List<WcSession>>,
    private val openAppInfoModal: (WcSession) -> Unit,
) : Transformer<WcConnectionsState> {

    override fun transform(prevState: WcConnectionsState): WcConnectionsState {
        return prevState.copy(
            connections = sessionsMap.map { (wallet, sessions) ->
                WcConnectionsUM(
                    userWalletId = wallet.walletId.stringValue,
                    walletName = wallet.name,
                    connectedApps = sessions.map { session ->
                        with(session.sdkModel) {
                            WcConnectedAppInfo(
                                name = appMetaData.name,
                                iconUrl = appMetaData.icons.firstOrNull().orEmpty(),
                                subtitle = WcAppSubtitleConverter.convert(session.sdkModel.appMetaData),
                                verifiedState = WcDAppVerifiedStateConverter {}
                                    .convert(session.securityStatus to appMetaData.name),
                                onClick = { openAppInfoModal(session) },
                            )
                        }
                    }.toPersistentList(),
                )
            }.toPersistentList(),
        )
    }
}