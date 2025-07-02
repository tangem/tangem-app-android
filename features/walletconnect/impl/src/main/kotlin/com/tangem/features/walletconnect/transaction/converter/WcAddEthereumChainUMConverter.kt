package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.usecase.method.WcAddNetworkUseCase
import com.tangem.features.walletconnect.transaction.entity.chain.WcAddEthereumChainItemUM
import com.tangem.features.walletconnect.transaction.entity.chain.WcAddEthereumChainUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcAddEthereumChainUMConverter @Inject constructor(
    private val basicBlocksConverter: BasicTransactionRequestInfoBlocksConverter,
    private val appInfoContentUMConverter: WcTransactionAppInfoContentUMConverter,
) : Converter<WcAddEthereumChainUMConverter.Input, WcAddEthereumChainUM> {

    override fun convert(value: Input): WcAddEthereumChainUM {
        return WcAddEthereumChainUM(
            transaction = WcAddEthereumChainItemUM(
                onDismiss = value.actions.onDismiss,
                onSign = value.actions.onSign,
                appInfo = appInfoContentUMConverter.convert(
                    WcTransactionAppInfoContentUMConverter.Input(
                        session = value.useCase.session,
                        onShowVerifiedAlert = value.actions.onShowVerifiedAlert,
                    ),
                ),
                walletName = value.useCase.session.wallet.name,
                isLoading = false,
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(
                blocks = basicBlocksConverter.convert(value.useCase),
                onCopy = value.actions.onCopy,
            ),
        )
    }

    data class Input(
        val useCase: WcAddNetworkUseCase,
        val actions: WcTransactionActionsUM,
    )
}