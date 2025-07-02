package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionItemUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal class WcSendTransactionUMConverter @Inject constructor(
    private val appInfoContentUMConverter: WcTransactionAppInfoContentUMConverter,
    private val networkInfoUMConverter: WcNetworkInfoUMConverter,
    private val requestBlockUMConverter: WcTransactionRequestBlockUMConverter,
) : Converter<WcSendTransactionUMConverter.Input, WcSendTransactionUM?> {

    override fun convert(value: Input): WcSendTransactionUM? = when (value.useCase.method) {
        is WcEthMethod.SendTransaction,
        is WcEthMethod.SignTransaction,
        is WcSolanaMethod.SignAllTransaction,
        is WcSolanaMethod.SignTransaction,
        -> WcSendTransactionUM(
            transaction = WcSendTransactionItemUM(
                onDismiss = value.actions.onDismiss,
                onSend = value.actions.onSign,
                appInfo = appInfoContentUMConverter.convert(
                    WcTransactionAppInfoContentUMConverter.Input(
                        session = value.useCase.session,
                        onShowVerifiedAlert = value.actions.onShowVerifiedAlert,
                    ),
                ),
                walletName = value.useCase.session.wallet.name,
                networkInfo = networkInfoUMConverter.convert(value.useCase.network),
                address = value.useCase.walletAddress.toShortAddressText(),
                estimatedWalletChanges = WcSendReceiveTransactionCheckResultsUM(),
                isLoading = value.signState.domainStep == WcSignStep.Signing,
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(
                blocks = buildList {
                    add(
                        requestBlockUMConverter.convert(
                            WcTransactionRequestBlockUMConverter.Input(value.useCase.rawSdkRequest),
                        ),
                    )
                    (value.useCase.method as? WcEthMethod.SendTransaction)?.transaction?.let { transaction ->
                        add(
                            WcTransactionRequestBlockUM(
                                info = buildList {
                                    transaction.to?.let {
                                        add(
                                            WcTransactionRequestInfoItemUM(
                                                title = resourceReference(R.string.wc_transaction_info_to_title),
                                                description = it,
                                            ),
                                        )
                                    }
                                    add(
                                        WcTransactionRequestInfoItemUM(
                                            title = resourceReference(R.string.send_from_wallet_android),
                                            description = transaction.from,
                                        ),
                                    )
                                }.toImmutableList(),
                            ),
                        )
                    }
                }.toImmutableList(),
                onCopy = value.actions.onCopy,
            ),
        )
        else -> null
    }

    data class Input(
        val useCase: WcTransactionUseCase,
        val signState: WcSignState<*>,
        val actions: WcTransactionActionsUM,
    )
}