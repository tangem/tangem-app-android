package com.tangem.data.pay.converter

import com.tangem.data.pay.entity.TangemPayCurrencyFactory
import arrow.core.getOrElse
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
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
            is PaymentAccountStatusValue.Locked -> PaymentAccountStatusValueDM.ActiveCard(
                isLocked = true,
                customerId = value.customerId,
                cardId = value.cardId,
                lastFourDigits = value.lastFourDigits,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                isPinSet = value.isPinSet,
                fiatBalance = value.fiatBalance.toDM(),
                cryptoBalance = value.cryptoBalance.toDM(),
                displayName = value.displayName?.value,
            )
            is PaymentAccountStatusValue.Loaded -> PaymentAccountStatusValueDM.ActiveCard(
                isLocked = false,
                customerId = value.customerId,
                cardId = value.cardId,
                lastFourDigits = value.lastFourDigits,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                isPinSet = value.isPinSet,
                fiatBalance = value.fiatBalance.toDM(),
                cryptoBalance = value.cryptoBalance.toDM(),
                displayName = value.displayName?.value,
            )
            is PaymentAccountStatusValue.Error.CardIssueFailed -> PaymentAccountStatusValueDM.CardIssueFailed(
                customerId = value.customerId,
            )
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
            is PaymentAccountStatusValueDM.NotCreated -> PaymentAccountStatusValue.NotCreated
            is PaymentAccountStatusValueDM.CardIssueFailed -> PaymentAccountStatusValue.Error.CardIssueFailed(
                customerId = value.customerId,
            )
            is PaymentAccountStatusValueDM.IssuingCard -> PaymentAccountStatusValue.IssuingCard(
                source = StatusSource.CACHE,
            )
            is PaymentAccountStatusValueDM.ActiveCard -> if (value.isLocked) {
                PaymentAccountStatusValue.Locked(
                    source = StatusSource.CACHE,
                    customerId = value.customerId,
                    cardId = value.cardId,
                    lastFourDigits = value.lastFourDigits,
                    currencyCode = value.currencyCode,
                    depositAddress = value.depositAddress,
                    isPinSet = value.isPinSet,
                    fiatBalance = value.fiatBalance.toDomain(),
                    cryptoBalance = value.cryptoBalance.toDomain(),
                    cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                    displayName = value.displayName?.let { CardDisplayName(it).getOrElse { null } },
                )
            } else {
                PaymentAccountStatusValue.Loaded(
                    source = StatusSource.CACHE,
                    customerId = value.customerId,
                    cardId = value.cardId,
                    lastFourDigits = value.lastFourDigits,
                    currencyCode = value.currencyCode,
                    depositAddress = value.depositAddress,
                    isPinSet = value.isPinSet,
                    fiatBalance = value.fiatBalance.toDomain(),
                    cryptoBalance = value.cryptoBalance.toDomain(),
                    cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                    displayName = value.displayName?.let { CardDisplayName(it).getOrElse { null } },
                )
            }
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