package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.domain.pay.model.bankofframp.OfframpToken
import com.tangem.spend.datasource.pay.models.response.BankOfframpOptionsResponse
import com.tangem.utils.converter.Converter
import java.util.Locale

/**
 * Converts the off-ramp routes. The token catalog and the rail list are backend configuration rather than a
 * closed contract, so an entry this app cannot describe — an unknown rail, a token without a contract address,
 * a rail without a fee or an ETA — is dropped instead of failing the whole response.
 */
internal object BankOfframpOptionsConverter :
    Converter<BankOfframpOptionsResponse.Result, BankOfframpOptions> {

    override fun convert(value: BankOfframpOptionsResponse.Result) = BankOfframpOptions(
        sources = value.sources.orEmpty().mapNotNull(::convertSource),
        destinations = value.destinations.orEmpty().mapNotNull(::convertDestination),
    )

    private fun convertSource(value: BankOfframpOptionsResponse.Source): OfframpSource? {
        val tokens = value.tokens.orEmpty().mapNotNull(::convertToken).ifEmpty { return null }
        return OfframpSource(
            networkName = value.name?.takeIf(String::isNotBlank) ?: return null,
            isTestnet = value.isTestnet == true,
            chainId = value.chainId ?: return null,
            tokens = tokens,
        )
    }

    private fun convertToken(value: BankOfframpOptionsResponse.Token): OfframpToken? {
        return OfframpToken(
            symbol = value.symbol?.takeIf(String::isNotBlank) ?: return null,
            contractAddress = value.contractAddress?.takeIf(String::isNotBlank) ?: return null,
        )
    }

    private fun convertDestination(value: BankOfframpOptionsResponse.Destination): OfframpDestination? {
        val fee = value.fee ?: return null
        val eta = value.eta ?: return null
        return OfframpDestination(
            rail = convertRail(value.rail) ?: return null,
            fee = OfframpFee(
                amount = fee.amount ?: return null,
                currency = fee.currency?.takeIf(String::isNotBlank) ?: return null,
                description = fee.description?.takeIf(String::isNotBlank),
            ),
            eta = OfframpEta(
                minDays = eta.minDays ?: return null,
                maxDays = eta.maxDays ?: return null,
            ),
        )
    }

    private fun convertRail(value: String?): BankRail? = when (value?.uppercase(Locale.US)) {
        BankRail.ACH.name -> BankRail.ACH
        BankRail.WIRE.name -> BankRail.WIRE
        else -> null
    }
}