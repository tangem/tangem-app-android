package com.tangem.data.walletconnect.pay

import com.reown.walletkit.client.Wallet
import com.tangem.domain.walletconnect.model.pay.*

internal object WcPayModelConverter {

    fun Wallet.Model.PaymentOptionsResponse.toDomain() = WcPaymentOptionsResponse(
        paymentId = paymentId,
        info = info?.toDomain(),
        options = options.map { it.toDomain() },
        collectDataAction = collectDataAction?.toDomain(),
    )

    private fun Wallet.Model.PaymentInfo.toDomain() = WcPaymentInfo(
        status = status.toDomain(),
        amount = amount.toDomain(),
        expiresAt = expiresAt,
        merchant = WcPayMerchantInfo(
            name = merchant.name,
            iconUrl = merchant.iconUrl,
        ),
    )

    private fun Wallet.Model.PaymentOption.toDomain() = WcPaymentOption(
        id = id,
        amount = amount.toDomain(),
        account = account,
        estimatedTxs = estimatedTxs,
        collectData = collectData?.toDomain(),
    )

    private fun Wallet.Model.PaymentAmount.toDomain() = WcPayAmount(
        value = value,
        unit = unit,
        display = display?.let {
            WcPayAmount.Display(
                assetSymbol = it.assetSymbol,
                assetName = it.assetName,
                decimals = it.decimals,
                iconUrl = it.iconUrl,
                networkName = it.networkName,
                networkIconUrl = it.networkIconUrl,
            )
        },
    )

    private fun Wallet.Model.CollectDataAction.toDomain() = WcPayCollectData(
        url = url,
        schema = schema,
    )

    fun Wallet.Model.RequiredAction.WalletRpc.toDomain() = WcPayRequiredAction(
        chainId = action.chainId,
        method = action.method,
        params = action.params,
    )

    private fun Wallet.Model.PaymentStatus.toDomain(): WcPaymentStatus = when (this) {
        Wallet.Model.PaymentStatus.REQUIRES_ACTION -> WcPaymentStatus.REQUIRES_ACTION
        Wallet.Model.PaymentStatus.PROCESSING -> WcPaymentStatus.PROCESSING
        Wallet.Model.PaymentStatus.SUCCEEDED -> WcPaymentStatus.SUCCEEDED
        Wallet.Model.PaymentStatus.FAILED -> WcPaymentStatus.FAILED
        Wallet.Model.PaymentStatus.EXPIRED -> WcPaymentStatus.EXPIRED
        Wallet.Model.PaymentStatus.CANCELLED -> WcPaymentStatus.CANCELLED
    }

    fun Wallet.Model.ConfirmPaymentResponse.toDomain() = WcPayConfirmResult(
        status = status.toDomain(),
        isFinal = isFinal,
        pollInMs = pollInMs,
        txId = info?.txId,
        optionAmount = info?.optionAmount?.toDomain(),
    )
}