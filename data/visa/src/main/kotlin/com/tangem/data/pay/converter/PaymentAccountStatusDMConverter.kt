package com.tangem.data.pay.converter

import com.tangem.data.pay.converter.PaymentAccountStatusDMConverter.convert
import com.tangem.data.pay.converter.PaymentAccountStatusDMConverter.convertBack
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.pay.PaymentAccountStatus
import com.tangem.utils.converter.TwoWayConverter

/**
 * Two-way converter between [PaymentAccountStatus] and [PaymentAccountStatusDM].
 *
 * [convert] maps domain → data model. Returns null for transient statuses that should not be persisted
 * (Loading, ExposedDevice, Unavailable, NotSynced).
 *
 * [convertBack] maps data model → domain. All restored statuses have [StatusSource.CACHE] as source.
 */
internal object PaymentAccountStatusDMConverter :
    TwoWayConverter<PaymentAccountStatus, PaymentAccountStatusDM?> {

    override fun convert(value: PaymentAccountStatus): PaymentAccountStatusDM? {
        return when (value) {
            is PaymentAccountStatus.NotCreated -> PaymentAccountStatusDM.NotCreated()
            is PaymentAccountStatus.UnderReview -> PaymentAccountStatusDM.UnderReview(kycStatus = value.kycStatus)
            is PaymentAccountStatus.IssuingCard -> PaymentAccountStatusDM.IssuingCard()
            is PaymentAccountStatus.Locked -> PaymentAccountStatusDM.Locked()
            is PaymentAccountStatus.Loaded -> PaymentAccountStatusDM.Loaded(
                cardId = value.cardId,
                lastFourDigits = value.lastFourDigits,
                balance = value.balance,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                isPinSet = value.isPinSet,
            )
            is PaymentAccountStatus.Error.CardIssueFailed -> PaymentAccountStatusDM.CardIssueFailed()
            // Transient statuses are not persisted
            is PaymentAccountStatus.Loading,
            is PaymentAccountStatus.Error.ExposedDevice,
            is PaymentAccountStatus.Error.Unavailable,
            is PaymentAccountStatus.Error.NotSynced,
            -> null
        }
    }

    override fun convertBack(value: PaymentAccountStatusDM?): PaymentAccountStatus {
        return when (value) {
            is PaymentAccountStatusDM.CardIssueFailed -> PaymentAccountStatus.Error.CardIssueFailed
            is PaymentAccountStatusDM.NotCreated -> PaymentAccountStatus.NotCreated
            is PaymentAccountStatusDM.IssuingCard -> PaymentAccountStatus.IssuingCard(source = StatusSource.CACHE)
            is PaymentAccountStatusDM.Locked -> PaymentAccountStatus.Locked(source = StatusSource.CACHE)
            is PaymentAccountStatusDM.UnderReview -> PaymentAccountStatus.UnderReview(
                source = StatusSource.CACHE,
                kycStatus = value.kycStatus,
            )
            is PaymentAccountStatusDM.Loaded -> PaymentAccountStatus.Loaded(
                source = StatusSource.CACHE,
                cardId = value.cardId,
                lastFourDigits = value.lastFourDigits,
                balance = value.balance,
                currencyCode = value.currencyCode,
                depositAddress = value.depositAddress,
                isPinSet = value.isPinSet,
            )
            null -> PaymentAccountStatus.Error.Unavailable(source = StatusSource.CACHE)
        }
    }
}