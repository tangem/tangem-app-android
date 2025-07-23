package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionItemUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
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
                    session = value.useCase.session,
                    onShowVerifiedAlert = value.actions.onShowVerifiedAlert,
                ),
            ),
            walletName = value.useCase.session.wallet.name.takeIf { value.useCase.session.showWalletInfo },
            networkInfo = networkInfoUMConverter.convert(value.useCase.network),
            isLoading = value.signState.domainStep == WcSignStep.Signing,
        ),
        transactionRequestInfo = WcTransactionRequestInfoUM(
            persistentListOf(
                requestBlockUMConverter.convert(
                    WcTransactionRequestBlockUMConverter.Input(value.useCase.rawSdkRequest, value.signModel),
                ),
            ),
            onCopy = value.actions.onCopy,
        ),
    )

    data class Input(
        val useCase: WcMessageSignUseCase,
        val signState: WcSignState<*>,
        val signModel: WcMessageSignUseCase.SignModel,
        val actions: WcTransactionActionsUM,
    )
}