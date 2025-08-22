package com.tangem.data.visa.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.card.common.util.derivationStyleProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.lib.visa.model.VisaContractInfo
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

internal class VisaCurrencyFactory @Inject constructor(
    private val cryptoCurrencyFactory: CryptoCurrencyFactory,
    private val networkFactory: NetworkFactory,
) {

    fun create(userWallet: UserWallet, contractInfo: VisaContractInfo, fiatRate: BigDecimal): VisaCurrency {
        val now = Instant.now()
        val currentLimit = if (contractInfo.limitsChangeDate > now) {
            contractInfo.oldLimits
        } else {
            contractInfo.newLimits
        }
        val remainingOtpLimit = getRemainingOtp(currentLimit, now)

        val currencyNetwork = networkFactory.create(
            blockchain = Blockchain.Polygon,
            extraDerivationPath = null,
            derivationStyleProvider = userWallet.requireColdWallet().scanResponse.derivationStyleProvider,
            canHandleTokens = true,
        ) ?: error("Unable to create network for Visa currency")

        val cryptoCurrency = cryptoCurrencyFactory.createToken(
            network = currencyNetwork,
            rawId = CryptoCurrency.RawID(VisaConstants.TOKEN_ID),
            name = contractInfo.token.name,
            symbol = contractInfo.token.symbol,
            decimals = contractInfo.token.decimals,
            contractAddress = contractInfo.token.address,
        )

        return VisaCurrency(
            cryptoCurrency = cryptoCurrency,
            symbol = contractInfo.token.symbol,
            networkName = VisaConstants.NETWORK_NAME,
            decimals = contractInfo.token.decimals,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO, // [Second Visa Iteration] pass to create() params if needed
            fiatCurrency = VisaConstants.fiatCurrency,
            balances = with(contractInfo) {
                VisaCurrency.Balances(
                    total = balances.total,
                    verified = balances.verified,
                    available = balances.available.forPayment,
                    blocked = balances.blocked,
                    debt = balances.debt,
                )
            },
            limits = VisaCurrency.Limits(
                remainingOtp = remainingOtpLimit,
                remainingNoOtp = minOf(remainingOtpLimit, getRemainingNoOtp(currentLimit, now)),
                singleTransaction = currentLimit.singleTransactionLimit,
                expirationDate = getLimitsExpirationDate(currentLimit, now),
            ),
            paymentAccountAddress = NetworkAddress.Single(
                NetworkAddress.Address(
                    value = contractInfo.paymentAccountAddress,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
        )
    }

    private fun getRemainingOtp(currentLimit: VisaContractInfo.Limits, now: Instant): BigDecimal {
        if (currentLimit.expirationDate >= now) {
            return currentLimit.spendLimit.limit - currentLimit.spendLimit.spent
        }

        return currentLimit.spendLimit.limit
    }

    private fun getRemainingNoOtp(currentLimit: VisaContractInfo.Limits, now: Instant): BigDecimal {
        if (currentLimit.expirationDate >= now) {
            return currentLimit.noOtpLimit.limit - currentLimit.noOtpLimit.spent
        }

        return currentLimit.noOtpLimit.limit
    }

    private fun getLimitsExpirationDate(currentLimits: VisaContractInfo.Limits, now: Instant): DateTime {
        val expirationDate = if (currentLimits.expirationDate >= now) {
            currentLimits.expirationDate.toDateTime()
        } else {
            val spendPeriodDays = currentLimits.spendPeriodSeconds
                .div(BigInteger.valueOf(SECONDS_IN_DAY))
                .toInt()

            now.toDateTime().plusDays(spendPeriodDays)
        }

        return expirationDate.withZone(DateTimeZone.getDefault())
    }

    private companion object {
        const val SECONDS_IN_DAY = 86_400L
    }
}