package com.tangem.data.pay.converter

import com.tangem.data.pay.util.tangemPayImageOrNull
import com.tangem.domain.models.pay.TangemPayImage
import arrow.core.getOrElse
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.*
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
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
                currencyCode = value.balance?.fiatBalance?.currency,
                paymentAccountAddress = value.paymentAccountAddress,
                fiatBalance = value.balance?.fiatBalance?.toDM(),
                fiatRate = value.fiatRate,
                cards = value.cards.map { card ->
                    PaymentAccountStatusValueDM.TangemPayCard(
                        id = card.id,
                        productInstanceId = card.productInstanceId,
                        cardStatus = card.cardStatus.name,
                        hasPinCode = card.hasPinCode,
                        displayName = card.displayName?.value,
                        actualDailyLimit = card.limit?.actualCardLimit?.amount,
                        adminDailyLimit = card.limit?.adminCardLimit?.amount,
                        frozenState = card.frozenState.toString(),
                        lastDigits = card.lastDigits,
                        images = card.images.map { image ->
                            PaymentAccountStatusValueDM.ImageDM(
                                type = image.type,
                                url = image.url,
                            )
                        },
                        state = card.state.toString(),
                        embossName = card.embossName,
                        cardType = card.cardType.name,
                    )
                },
            )
            is PaymentAccountStatusValue.Error.CardIssueFailed -> PaymentAccountStatusValueDM.CardIssueFailed(
                customerId = value.customerId,
                fiatRate = value.fiatRate,
                balance = value.balance?.toDM(),
            )
            is PaymentAccountStatusValue.Empty -> PaymentAccountStatusValueDM.Empty()
            is PaymentAccountStatusValue.Deactivated -> PaymentAccountStatusValueDM.DeactivatedAccount(
                customerId = value.customerId,
                fiatRate = value.fiatRate,
                fiatBalance = value.balance?.fiatBalance?.toDM(),
            )
            // Transient statuses are not persisted
            is PaymentAccountStatusValue.Loading,
            is PaymentAccountStatusValue.AwaitingPlanSelection,
            is PaymentAccountStatusValue.Inactive,
            is PaymentAccountStatusValue.Error.ExposedDevice,
            is PaymentAccountStatusValue.Error.Unavailable,
            is PaymentAccountStatusValue.Error.NotSynced,
            -> null
        }
    }

    fun convertBack(userWalletId: UserWalletId, value: PaymentAccountStatusValueDM?): PaymentAccountStatusValue {
        val cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId)
        return when (value) {
            is PaymentAccountStatusValueDM.Empty -> PaymentAccountStatusValue.Empty
            is PaymentAccountStatusValueDM.NotCreated -> PaymentAccountStatusValue.NotCreated
            is PaymentAccountStatusValueDM.CardIssueFailed -> value.toDomain()
            is PaymentAccountStatusValueDM.IssuingCard -> PaymentAccountStatusValue.IssuingCard(
                source = StatusSource.CACHE,
            )
            is PaymentAccountStatusValueDM.ActiveAccount -> PaymentAccountStatusValue.Loaded(
                source = StatusSource.CACHE,
                customerId = value.customerId,
                paymentAccountAddress = value.paymentAccountAddress,
                balance = value.getBalance(),
                cryptoCurrency = cryptoCurrency,
                networks = emptyList(),
                fiatRate = value.fiatRate,
                cards = value.cards.map { card ->
                    TangemPayCard(
                        id = card.id,
                        productInstanceId = card.productInstanceId,
                        cardStatus = TangemPayCard.Status.fromString(card.cardStatus),
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
                        frozenState = TangemPayCardFrozenState.fromString(card.frozenState),
                        lastDigits = card.lastDigits,
                        images = card.getImages(),
                        state = TangemPayCardState.fromString(card.state),
                        embossName = card.embossName,
                        cardType = TangemPayCardType.fromString(card.cardType),
                    )
                },
                error = null,
                virtualAccount = null,
                tariffPlan = null,
            )
            is PaymentAccountStatusValueDM.UnderReview -> PaymentAccountStatusValue.UnderReview(
                source = StatusSource.CACHE,
                kycStatus = value.kycStatus,
                customerId = value.customerId,
            )
            is PaymentAccountStatusValueDM.DeactivatedAccount -> PaymentAccountStatusValue.Deactivated(
                source = StatusSource.CACHE,
                customerId = value.customerId,
                balance = value.fiatBalance?.toDomain()?.let(PaymentAccountStatusValue::Balance),
                cryptoCurrency = cryptoCurrency,
                networks = emptyList(),
                fiatRate = value.fiatRate,
                error = null,
            )
            null -> PaymentAccountStatusValue.Error.Unavailable
        }
    }

    private fun PaymentAccountStatusValue.Balance.toDM() = PaymentAccountStatusValueDM.BalanceDM(
        fiatBalance = fiatBalance.toDM(),
    )

    private fun PaymentAccountStatusValueDM.BalanceDM.toDomain() = PaymentAccountStatusValue.Balance(
        fiatBalance = fiatBalance.toDomain(),
    )

    private fun PaymentAccountStatusValue.FiatBalance.toDM(): PaymentAccountStatusValueDM.FiatBalanceDM {
        return PaymentAccountStatusValueDM.FiatBalanceDM(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun PaymentAccountStatusValueDM.ActiveAccount.getBalance(): PaymentAccountStatusValue.Balance? =
        fiatBalance?.toDomain()?.let(PaymentAccountStatusValue::Balance)

    private fun PaymentAccountStatusValueDM.CardIssueFailed.toDomain() =
        PaymentAccountStatusValue.Error.CardIssueFailed(
            customerId = customerId,
            source = StatusSource.CACHE,
            balance = balance?.toDomain(),
            fiatRate = fiatRate,
        )

    private fun PaymentAccountStatusValueDM.FiatBalanceDM.toDomain(): PaymentAccountStatusValue.FiatBalance {
        return PaymentAccountStatusValue.FiatBalance(
            availableBalance = availableBalance,
            currency = currency,
        )
    }

    private fun PaymentAccountStatusValueDM.TangemPayCard.getImages(): List<TangemPayImage> {
        return images.mapNotNull { image -> tangemPayImageOrNull(image.type, image.url) }
    }
}