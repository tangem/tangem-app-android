package com.tangem.features.walletconnect.transaction.ui.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

private const val DECIMALS_AMOUNT = 2

internal class WcSendAndReceiveBlockAidUiConverter @Inject constructor() :
    Converter<CheckTransactionResult, WcSendReceiveTransactionCheckResultsUM> {
    override fun convert(value: CheckTransactionResult): WcSendReceiveTransactionCheckResultsUM {
        return WcSendReceiveTransactionCheckResultsUM(
            isLoading = false,
            notificationText = when (value.validation) {
                ValidationResult.SAFE, ValidationResult.FAILED_TO_VALIDATE -> null
                ValidationResult.UNSAFE -> TextReference.Str("") // TODO("Add text after approve with designer")
            },
            estimatedWalletChanges = (value.simulation as? SimulationResult.Success)?.data?.let { data ->
                when (data) {
                    is SimulationData.Approve -> null
                    is SimulationData.SendAndReceive -> {
                        val items: ImmutableList<WcEstimatedWalletChangeUM> = (
                            data.send.map {
                                when (it) {
                                    is AmountInfo.FungibleTokens -> it.toUM(
                                        iconRes = R.drawable.ic_send_new_24,
                                        titleRes = R.string.common_send,
                                    )
                                    is AmountInfo.NonFungibleTokens -> it.toUM(
                                        iconRes = R.drawable.ic_send_new_24,
                                        titleRes = R.string.common_send,
                                    )
                                }
                            } + data.receive.map {
                                when (it) {
                                    is AmountInfo.FungibleTokens -> it.toUM(
                                        iconRes = R.drawable.ic_receive_new_24,
                                        titleRes = R.string.common_receive,
                                    )
                                    is AmountInfo.NonFungibleTokens -> it.toUM(
                                        iconRes = R.drawable.ic_receive_new_24,
                                        titleRes = R.string.common_receive,
                                    )
                                }
                            }
                            ).toImmutableList()
                        WcEstimatedWalletChangesUM(items)
                    }
                }
            },
        )
    }
}

private fun AmountInfo.FungibleTokens.toUM(iconRes: Int, titleRes: Int): WcEstimatedWalletChangeUM {
    val amountText = amount.format { crypto(token.symbol, DECIMALS_AMOUNT) }
    return WcEstimatedWalletChangeUM(
        iconRes = iconRes,
        title = resourceReference(titleRes),
        description = "- $amountText",
        tokenIconUrl = token.logoUrl,
    )
}

private fun AmountInfo.NonFungibleTokens.toUM(iconRes: Int, titleRes: Int): WcEstimatedWalletChangeUM {
    return WcEstimatedWalletChangeUM(
        iconRes = iconRes,
        title = resourceReference(titleRes),
        description = name,
        tokenIconUrl = logoUrl,
    )
}