package com.tangem.data.networks.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyId
import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyId.Companion.CONTRACT_ADDRESS_DELIMITER
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.domain.models.currency.CryptoCurrency.ID.Suffix as CurrencyIdSuffix

/**
 * Converts between [CurrencyId] and [CryptoCurrency.ID].
 *
 * @property rawNetworkId the raw network ID associated with the currency
 * @property derivationPath the derivation path used for the network
 *
[REDACTED_AUTHOR]
 */
internal class CurrencyIdConverter(
    private val rawNetworkId: String,
    private val derivationPath: Network.DerivationPath,
) : TwoWayConverter<CurrencyId, CryptoCurrency.ID> {

    override fun convert(value: CurrencyId): CryptoCurrency.ID {
        val suffixParts = value.value.split(CONTRACT_ADDRESS_DELIMITER)

        val rawId = suffixParts.getOrNull(0)
        val contractAddress = suffixParts.getOrNull(1)

        return if (contractAddress.isNullOrBlank()) {
            getCoinId(
                coinId = rawId.takeUnless { it.isNullOrBlank() }
                    ?: error("Coin id is null for $rawNetworkId with $derivationPath"),
            )
        } else {
            getTokenId(
                rawTokenId = rawId?.ifBlank { null },
                contractAddress = contractAddress,
            )
        }
    }

    override fun convertBack(value: CryptoCurrency.ID): CurrencyId {
        return if (value.isCoin) {
            CurrencyId.createCoinId(
                coinId = Blockchain.fromId(value.rawNetworkId).toCoinId(),
            )
        } else {
            CurrencyId.createTokenId(
                rawTokenId = value.rawCurrencyId?.value,
                contractAddress = requireNotNull(value.contractAddress) {
                    "Token contractAddress is null for token id: $this"
                },
            )
        }
    }

    private fun getCoinId(coinId: String): CryptoCurrency.ID {
        return CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = getCurrencyIdBody(),
            suffix = CurrencyIdSuffix.RawID(rawId = coinId),
        )
    }

    private fun getTokenId(rawTokenId: String?, contractAddress: String): CryptoCurrency.ID {
        val suffix = if (rawTokenId == null) {
            CurrencyIdSuffix.ContractAddress(contractAddress)
        } else {
            CurrencyIdSuffix.RawID(rawTokenId, contractAddress)
        }

        return CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = getCurrencyIdBody(),
            suffix = suffix,
        )
    }

    private fun getCurrencyIdBody(): CryptoCurrency.ID.Body {
        return when (derivationPath) {
            is Network.DerivationPath.Card -> {
                CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = rawNetworkId,
                    derivationPath = derivationPath.value,
                )
            }
            is Network.DerivationPath.Custom -> {
                CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = rawNetworkId,
                    derivationPath = derivationPath.value,
                )
            }
            is Network.DerivationPath.None -> CryptoCurrency.ID.Body.NetworkId(rawNetworkId)
        }
    }
}