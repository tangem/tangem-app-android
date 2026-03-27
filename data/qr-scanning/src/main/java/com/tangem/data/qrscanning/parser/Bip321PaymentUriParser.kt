package com.tangem.data.qrscanning.parser

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent

internal class Bip321PaymentUriParser(
    private val blockchainDataProvider: QrContentClassifierParser.BlockchainDataProvider,
    private val helper: PaymentUriResolveHelper = PaymentUriResolveHelper(),
) : PaymentUriParser {

    override fun parse(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): PaymentUriParser.ParseResult {
        val (scheme, blockchains) = SCHEME_TO_BLOCKCHAINS.entries
            .firstOrNull { (scheme, _) -> qrCode.startsWith(scheme, ignoreCase = true) }
            ?: return PaymentUriParser.ParseResult.NotRecognized

        val parsed = helper.parseUri(qrCode, scheme)
            ?: return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )

        val matchingCoins = coins.filter { it.network.toBlockchain() in blockchains }
        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()
        val matchingCurrencies = allCurrencies.filter { it.network.id in matchingNetworkIds }

        if (matchingCurrencies.isEmpty()) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork(
                    raw = qrCode,
                    blockchain = blockchains.first().fullName,
                ),
            )
        }

        val isAddressValid = matchingCoins.any {
            blockchainDataProvider.validateAddress(it.network, parsed.address)
        }
        if (!isAddressValid) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )
        }

        val result = PaymentUriParser.ParseResult.Success(
            ClassifiedQrContent.PaymentUri(
                address = parsed.address,
                amount = parsed.amount,
                memo = parsed.memo?.second,
                matchingCurrencies = matchingCurrencies,
            ),
        )

        val unconsumed = parsed.remainingParams - PARAM_LABEL
        return helper.validateParams(
            result = result,
            unconsumedParams = unconsumed,
            memo = parsed.memo,
            matchingCoins = matchingCoins,
        )
    }

    private companion object {
        const val PARAM_LABEL = "label"
        val BLOCKCHAINS = listOf(
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Litecoin,
            Blockchain.Binance,
            Blockchain.BinanceTestnet,
            Blockchain.Dogecoin,
            Blockchain.XRP,
        )
        val SCHEME_TO_BLOCKCHAINS: Map<String, Set<Blockchain>> = BLOCKCHAINS
            .flatMap { blockchain -> blockchain.getShareScheme().map { scheme -> scheme to blockchain } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
    }
}