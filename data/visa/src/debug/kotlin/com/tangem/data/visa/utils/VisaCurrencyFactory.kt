package com.tangem.data.visa.utils

import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.lib.visa.model.VisaBalancesAndLimits
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import java.math.BigDecimal
import java.math.BigInteger

internal class VisaCurrencyFactory {

    fun create(balancesAndLimits: VisaBalancesAndLimits, fiatRate: BigDecimal?): VisaCurrency {
        val now = Instant.now()
        val currentLimit = if (balancesAndLimits.limitsChangeDate > now) {
            balancesAndLimits.oldLimits
        } else {
            balancesAndLimits.newLimits
        }
        val remainingOtpLimit = getRemainingOtp(currentLimit, now)

        return VisaCurrency(
            symbol = VisaConfig.TOKEN_SYMBOL,
            networkName = VisaConfig.NETWORK_NAME,
            decimals = VisaConfig.TOKEN_DECIMALS,
            fiatRate = fiatRate,
            fiatCurrency = VisaConfig.fiatCurrency,
            balances = with(balancesAndLimits) {
                VisaCurrency.Balances(
                    total = balances.total,
                    verified = balances.verified,
                    available = balances.available.forPayment,
                    blocked = balances.blocked,
                    debt = balances.debt,
                    pendingRefund = balances.pendingRefund,
                )
            },
            limits = VisaCurrency.Limits(
                remainingOtp = remainingOtpLimit,
                remainingNoOtp = minOf(remainingOtpLimit, getRemainingNoOtp(currentLimit, now)),
                singleTransaction = currentLimit.singleTransactionLimit,
                expirationDate = getLimitsExpirationDate(currentLimit, now),
            ),
        )
    }

    private fun getRemainingOtp(currentLimit: VisaBalancesAndLimits.Limits, now: Instant): BigDecimal {
        if (currentLimit.expirationDate >= now) {
            return currentLimit.spendLimit.limit - currentLimit.spendLimit.spent
        }

        return currentLimit.spendLimit.limit
    }

    private fun getRemainingNoOtp(currentLimit: VisaBalancesAndLimits.Limits, now: Instant): BigDecimal {
        if (currentLimit.expirationDate >= now) {
            return currentLimit.noOtpLimit.limit - currentLimit.noOtpLimit.spent
        }

        return currentLimit.noOtpLimit.limit
    }

    private fun getLimitsExpirationDate(currentLimits: VisaBalancesAndLimits.Limits, now: Instant): DateTime {
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