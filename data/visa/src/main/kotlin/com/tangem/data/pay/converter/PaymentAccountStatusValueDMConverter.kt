package com.tangem.data.pay.converter

import arrow.core.getOrElse
import com.tangem.data.pay.entity.TangemPayCurrencyFactory
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardLimit
import com.tangem.domain.models.pay.TangemPayCardLimitData
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way converter between [PaymentAccountStatusValue] and [PaymentAccountStatusValueDM].
 *
 * [convert] maps domain → data model. Returns null for transient statuses that should not be persisted
 * (Loading, ExposedDevice, Unavailable, NotSynced).
 *
 * [convertBack] maps data model → domain. All restored statuses have [StatusSource.CACHE] as source.
 */
@Singleton
internal class PaymentAccountStatusValueDMConverter @Inject constructor(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) {

    fun convert(value: PaymentAccountStatusValue): PaymentAccountStatusValueDM? {
        return when (value) {
            is PaymentAccountStatusValue.NotCreated -> PaymentAccountStatusValueDM.NotCreated()
            is PaymentAccountStatusValue.UnderReview -> PaymentAccountStatusValueDM.UnderReview(
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is PaymentAccountStatusValue.IssuingCard -> PaymentAccountStatusValueDM.IssuingCard()
            is PaymentAccountStatusValue.Loaded -> PaymentAccountStatusValueDM.ActiveAccount(
                customerId = value.customerId,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                fiatBalance = value.fiatBalance.toDM(),
                cryptoBalance = value.cryptoBalance.toDM(),
                cards = value.cards.map { card ->
                    PaymentAccountStatusValueDM.TangemPayCard(
                        id = card.id,
                        hasPinCode = card.hasPinCode,
                        displayName = card.displayName?.value,
                        actualDailyLimit = card.limit?.actualCardLimit?.amount,
                        adminDailyLimit = card.limit?.adminCardLimit?.amount,
                        isFrozen = card.isFrozen,
                        lastDigits = card.lastDigits,
                    )
                },
            )
            is PaymentAccountStatusValue.Error.CardIssueFailed -> PaymentAccountStatusValueDM.CardIssueFailed(
                customerId = value.customerId,
            )
            is PaymentAccountStatusValue.Empty -> PaymentAccountStatusValueDM.Empty()
            // Transient statuses are not persisted
            is PaymentAccountStatusValue.Loading,
            is PaymentAccountStatusValue.Error.ExposedDevice,
            is PaymentAccountStatusValue.Error.Unavailable,
            is PaymentAccountStatusValue.Error.NotSynced,
            -> null
        }
    }

    fun convertBack(userWalletId: UserWalletId, value: PaymentAccountStatusValueDM?): PaymentAccountStatusValue {
        return when (value) {
            is PaymentAccountStatusValueDM.Empty -> PaymentAccountStatusValue.Empty
            is PaymentAccountStatusValueDM.NotCreated -> PaymentAccountStatusValue.NotCreated
            is PaymentAccountStatusValueDM.CardIssueFailed -> PaymentAccountStatusValue.Error.CardIssueFailed(
                customerId = value.customerId,
            )
            is PaymentAccountStatusValueDM.IssuingCard -> PaymentAccountStatusValue.IssuingCard(
                source = StatusSource.CACHE,
            )
            is PaymentAccountStatusValueDM.ActiveAccount -> PaymentAccountStatusValue.Loaded(
                source = StatusSource.CACHE,
                customerId = value.customerId,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                fiatBalance = value.fiatBalance.toDomain(),
                cryptoBalance = value.cryptoBalance.toDomain(),
                cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                cards = value.cards.map { card ->
                    TangemPayCard(
                        id = card.id,
                        hasPinCode = card.hasPinCode,
                        displayName = card.displayName?.let { CardDisplayName(it).getOrElse { null } },
                        limit = TangemPayCardLimitData(
                            actualCardLimit = card.actualDailyLimit?.let { limit ->
                                TangemPayCardLimit(limit, TangemPayCardLimitPeriod.DAY)
                            },
                            adminCardLimit = card.adminDailyLimit?.let { limit ->
                                TangemPayCardLimit(limit, TangemPayCardLimitPeriod.DAY)
                            },
                        ),
                        isFrozen = card.isFrozen,
                        lastDigits = card.lastDigits,
                    )
                },
            )
            is PaymentAccountStatusValueDM.UnderReview -> PaymentAccountStatusValue.UnderReview(
                source = StatusSource.CACHE,
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            null -> PaymentAccountStatusValue.Error.Unavailable
        }
    }

    private fun PaymentAccountStatusValue.FiatBalance.toDM(): PaymentAccountStatusValueDM.FiatBalanceDM {
        return PaymentAccountStatusValueDM.FiatBalanceDM(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun PaymentAccountStatusValue.CryptoBalance.toDM(): PaymentAccountStatusValueDM.CryptoBalanceDM {
        return PaymentAccountStatusValueDM.CryptoBalanceDM(
            id = id,
            chainId = chainId,
            depositAddress = depositAddress,
            tokenContractAddress = tokenContractAddress,
            balance = balance,
        )
    }

    private fun PaymentAccountStatusValueDM.FiatBalanceDM.toDomain(): PaymentAccountStatusValue.FiatBalance {
        return PaymentAccountStatusValue.FiatBalance(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun PaymentAccountStatusValueDM.CryptoBalanceDM.toDomain(): PaymentAccountStatusValue.CryptoBalance {
        return PaymentAccountStatusValue.CryptoBalance(
            id = id,
            chainId = chainId,
            depositAddress = depositAddress,
            tokenContractAddress = tokenContractAddress,
            balance = balance,
        )
    }
}