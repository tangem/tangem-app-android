package com.tangem.data.qrscanning.parser

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent

internal class SolanaPaymentUriParser(
    private val helper: PaymentUriResolveHelper = PaymentUriResolveHelper(),
) : PaymentUriParser {

    override fun parse(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): PaymentUriParser.ParseResult {
        val scheme = SCHEMES.find { qrCode.startsWith(it, ignoreCase = true) }
            ?: return PaymentUriParser.ParseResult.NotRecognized

        val parsed = helper.parseUri(qrCode, scheme)
            ?: return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )

        val matchingCoins = coins.filter { it.network.toBlockchain() == BLOCKCHAIN }
        if (matchingCoins.isEmpty()) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork(raw = qrCode, blockchain = BLOCKCHAIN.fullName),
            )
        }

        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()
        val context = PaymentUriResolveHelper.ResolveContext(
            parsed = parsed,
            matchingCoins = matchingCoins,
            matchingNetworkIds = matchingNetworkIds,
            allCurrencies = allCurrencies,
            blockchainName = BLOCKCHAIN.fullName,
            qrCode = qrCode,
        )

        val splTokenMint = parsed.params[PARAM_SPL_TOKEN]
        return if (splTokenMint != null) {
            helper.resolveTokenTransfer(context, splTokenMint)
        } else {
            helper.resolveNativeOrAll(context)
        }
    }

    private companion object {
        val BLOCKCHAIN = Blockchain.Solana
        val SCHEMES = BLOCKCHAIN.getShareScheme()
        const val PARAM_SPL_TOKEN = "spl-token"
    }
}