package com.tangem.data.collectibles.account.converter

import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue

/**
 * Maps between the [com.tangem.domain.models.account.CollectiblesAccountStatusValue] domain model and the [com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO]
 * storage DTO.
 *
 * [convert] returns `null` for every state that does not mean "there is an account" — that `null` is what makes
 * the repository refuse the write, so a loading or failed read cannot be recorded as the account itself.
 *
 * [convertBack] always restores the value as [com.tangem.domain.models.StatusSource.CACHE]: whatever it was when it was written, it has
 * not been re-read from the chain in this session.
 */
internal object CollectiblesAccountStatusValueDTOConverter {

    fun convert(value: CollectiblesAccountStatusValue): CollectiblesAccountStatusValueDTO? = when (value) {
        is CollectiblesAccountStatusValue.Onboarded -> CollectiblesAccountStatusValueDTO.ONBOARDED
        is CollectiblesAccountStatusValue.UpdateRequired -> CollectiblesAccountStatusValueDTO.UPDATE_REQUIRED
        is CollectiblesAccountStatusValue.Loading,
        is CollectiblesAccountStatusValue.NotOnboarded,
        is CollectiblesAccountStatusValue.Error,
        -> null
    }

    fun convertBack(value: CollectiblesAccountStatusValueDTO): CollectiblesAccountStatusValue = when (value) {
        CollectiblesAccountStatusValueDTO.ONBOARDED -> CollectiblesAccountStatusValue.Onboarded(
            source = StatusSource.CACHE,
        )
        CollectiblesAccountStatusValueDTO.UPDATE_REQUIRED -> CollectiblesAccountStatusValue.UpdateRequired(
            source = StatusSource.CACHE,
        )
    }
}