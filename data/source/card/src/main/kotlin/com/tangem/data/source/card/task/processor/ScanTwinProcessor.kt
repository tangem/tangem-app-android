package com.tangem.data.source.card.task.processor

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.extensions.toHexString
import com.tangem.data.source.card.model.ProductType
import com.tangem.data.source.card.model.ScanResult
import com.tangem.data.source.card.utils.TWINS_PUBLIC_KEY_LENGTH
import com.tangem.data.source.card.utils.verifyTwinPublicKey
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand

internal class ScanTwinProcessor : ProductCommandProcessor<ScanResult> {
    override fun proceed(card: Card, session: CardSession, callback: (result: CompletionResult<ScanResult>) -> Unit) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.wallets.firstOrNull()?.publicKey
                    if (publicKey == null) {
                        val response = ScanResult(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = null,
                        )
                        callback(CompletionResult.Success(response))
                        return@run
                    }

                    val verified = verifyTwinPublicKey(readDataResult.data.issuerData, publicKey)
                    val response = if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData
                            .sliceArray(0 until TWINS_PUBLIC_KEY_LENGTH)
                        val walletData = session.environment.walletData
                        ScanResult(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = walletData,
                            secondTwinPublicKey = twinPublicKey.toHexString(),
                        )
                    } else {
                        ScanResult(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = null,
                        )
                    }
                    callback(CompletionResult.Success(response))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Success(ScanResult(card, ProductType.Twins, null)))
                }
            }
        }
    }
}
