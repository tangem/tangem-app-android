package com.tangem.features.walletconnect.transaction.utils

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.WcSignTransactionUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionRequestInfoItemUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.WcTransactionUM
import kotlinx.collections.immutable.persistentListOf

internal fun WcSignUseCase.toUM(signState: WcSignState<*>, actions: WcTransactionActionsUM): WcSignTransactionUM? {
    return when (this) {
        is WcMessageSignUseCase -> {
            ethMessageSignToUM(
                signState = signState,
                signModel = signState.signModel as WcMessageSignUseCase.SignModel,
                actions = actions,
            )
        }
        else -> null
    }
}

private fun WcMessageSignUseCase.ethMessageSignToUM(
    signState: WcSignState<*>,
    signModel: WcMessageSignUseCase.SignModel,
    actions: WcTransactionActionsUM,
) = WcSignTransactionUM(
    startIconRes = R.drawable.ic_back_24,
    endIconRes = R.drawable.ic_close_24,
    transactionIconRes = R.drawable.ic_doc_new_24,
    state = WcSignTransactionUM.State.TRANSACTION,
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
    ),
)