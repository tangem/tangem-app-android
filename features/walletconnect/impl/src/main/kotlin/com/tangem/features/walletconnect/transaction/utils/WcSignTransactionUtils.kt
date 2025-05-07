package com.tangem.features.walletconnect.transaction.utils

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.WcSignTransactionUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionRequestInfoItemUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal fun WcSignUseCase.toUM(signState: WcSignState<*>, actions: WcTransactionActionsUM): WcSignTransactionUM? {
    return when (this) {
        is WcMessageSignUseCase -> {
            when (method) {
                is WcEthMethod.SignTypedData -> signTypedDataToUM(
                    signState = signState,
                    signModel = signState.signModel as WcMessageSignUseCase.SignModel,
                    actions = actions,
                )
                is WcEthMethod.MessageSign, is WcSolanaMethod.SignMessage -> messageSignToUM(
                    signState = signState,
                    signModel = signState.signModel as WcMessageSignUseCase.SignModel,
                    actions = actions,
                )
                else -> null
            }
        }
        else -> null
    }
}

private fun WcMessageSignUseCase.signTypedDataToUM(
    signState: WcSignState<*>,
    signModel: WcMessageSignUseCase.SignModel,
    actions: WcTransactionActionsUM,
) = WcSignTransactionUM(
    actions = actions,
    transaction = WcTransactionUM(
        appName = session.sdkModel.appMetaData.name,
        appIcon = session.sdkModel.appMetaData.url,
        isVerified = session.securityStatus == CheckDAppResult.SAFE,
        appSubtitle = session.sdkModel.appMetaData.description,
        walletName = session.wallet.name,
        networkInfo = WcNetworkInfoUM(
            name = network.name,
            iconRes = getActiveIconRes(network.id.value),
        ),
        addressText = walletAddress.toShortAddressText(),
        isLoading = signState.domainStep == WcSignStep.Signing,
    ),
    transactionRequestInfo = WcTransactionRequestInfoUM(
        buildList {
            add(createInfoBlockUM(rawSdkRequest, signModel))
            (method as? WcEthMethod.SignTypedData)?.params?.message?.to?.let { to ->
                add(
                    WcTransactionRequestBlockUM(
                        persistentListOf(
                            WcTransactionRequestInfoItemUM(
                                title = resourceReference(R.string.wc_transaction_info_to_title),
                            ),
                            WcTransactionRequestInfoItemUM(
                                title = resourceReference(R.string.settings_wallet_name_title),
                                description = to.name,
                            ),
                            WcTransactionRequestInfoItemUM(
                                title = resourceReference(R.string.wc_common_wallet),
                                description = to.wallet,
                            ),
                        ),
                    ),
                )
            }
        }.toImmutableList(),
    ),
)

private fun WcMessageSignUseCase.messageSignToUM(
    signState: WcSignState<*>,
    signModel: WcMessageSignUseCase.SignModel,
    actions: WcTransactionActionsUM,
) = WcSignTransactionUM(
    actions = actions,
    transaction = WcTransactionUM(
        appName = session.sdkModel.appMetaData.name,
        appIcon = session.sdkModel.appMetaData.url,
        isVerified = session.securityStatus == CheckDAppResult.SAFE,
        appSubtitle = session.sdkModel.appMetaData.description,
        walletName = session.wallet.name,
        networkInfo = WcNetworkInfoUM(
            name = network.name,
            iconRes = getActiveIconRes(network.id.value),
        ),
        isLoading = signState.domainStep == WcSignStep.Signing,
    ),
    transactionRequestInfo = WcTransactionRequestInfoUM(
        persistentListOf(createInfoBlockUM(rawSdkRequest, signModel)),
    ),
)

private fun createInfoBlockUM(
    rawSdkRequest: WcSdkSessionRequest,
    signModel: WcMessageSignUseCase.SignModel,
): WcTransactionRequestBlockUM {
    return WcTransactionRequestBlockUM(
        persistentListOf(
            WcTransactionRequestInfoItemUM(
                title = resourceReference(R.string.wc_signature_type),
                description = rawSdkRequest.request.method,
            ),
            WcTransactionRequestInfoItemUM(
                title = resourceReference(R.string.wc_contents),
                description = signModel.humanMsg,
            ),
        ),
    )
}