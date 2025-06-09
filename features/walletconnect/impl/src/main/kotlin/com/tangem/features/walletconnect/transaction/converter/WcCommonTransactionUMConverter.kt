package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcCommonTransactionUMConverter @Inject constructor(
    private val signTypedDataUMConverter: WcSignTypedDataUMConverter,
    private val signTransactionUMConverter: WcSignTransactionUMConverter,
    private val sendTransactionUMConverter: WcSendTransactionUMConverter,
) : Converter<WcCommonTransactionUMConverter.Input, WcCommonTransactionUM?> {

    override fun convert(value: Input): WcCommonTransactionUM? {
        return when (value.useCase) {
            is WcMessageSignUseCase -> {
                when (value.useCase.method) {
                    is WcEthMethod.SignTypedData -> signTypedDataUMConverter.convert(
                        WcSignTypedDataUMConverter.Input(
                            useCase = value.useCase,
                            signState = value.signState,
                            signModel = value.signState.signModel as WcMessageSignUseCase.SignModel,
                            actions = value.actions,
                        ),
                    )
                    is WcEthMethod.MessageSign, is WcSolanaMethod.SignMessage -> signTransactionUMConverter.convert(
                        WcSignTransactionUMConverter.Input(
                            useCase = value.useCase,
                            signState = value.signState,
                            signModel = value.signState.signModel as WcMessageSignUseCase.SignModel,
                            actions = value.actions,
                        ),
                    )
                    else -> null
                }
            }
            is WcTransactionUseCase -> sendTransactionUMConverter.convert(
                WcSendTransactionUMConverter.Input(
                    useCase = value.useCase,
                    signState = value.signState,
                    actions = value.actions,
                ),
            )
            else -> null
        }
    }

    data class Input(
        val useCase: WcSignUseCase<*>,
        val signState: WcSignState<*>,
        val actions: WcTransactionActionsUM,
    )
}