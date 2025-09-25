package com.tangem.features.walletconnect.transaction.converter

import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcMethodContext
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionItemUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal class WcSignTransactionUMConverter @Inject constructor(
    private val appInfoContentUMConverter: WcTransactionAppInfoContentUMConverter,
    private val networkInfoUMConverter: WcNetworkInfoUMConverter,
    private val requestBlockUMConverter: WcTransactionRequestBlockUMConverter,
) : Converter<WcSignTransactionUMConverter.Input, WcSignTransactionUM> {

    override fun convert(value: Input) = WcSignTransactionUM(
        transaction = WcSignTransactionItemUM(
            onDismiss = value.actions.onDismiss,
            onSign = value.actions.onSign,
            appInfo = appInfoContentUMConverter.convert(
                WcTransactionAppInfoContentUMConverter.Input(
                    session = value.context.session,
                    onShowVerifiedAlert = value.actions.onShowVerifiedAlert,
                ),
            ),
            walletName = value.context.session.wallet.name.takeIf { value.context.session.showWalletInfo },
            networkInfo = networkInfoUMConverter.convert(value.context.network),
            isLoading = value.signState.domainStep == WcSignStep.Signing,
            address = WcAddressConverter.convert(value.context.derivationState),
            walletInteractionIcon = walletInterationIcon(value.context.session.wallet),
        ),
        transactionRequestInfo = WcTransactionRequestInfoUM(
            requestBlockUMConverter.convert(
                WcTransactionRequestBlockUMConverter.Input(value.context.rawSdkRequest, value.signModel),
            ).toImmutableList(),
            onCopy = value.actions.onCopy,
        ),
    )

    data class Input(
        val context: WcMethodContext,
        val signState: WcSignState<*>,
        val signModel: WcMessageSignUseCase.SignModel,
        val actions: WcTransactionActionsUM,
    )
}