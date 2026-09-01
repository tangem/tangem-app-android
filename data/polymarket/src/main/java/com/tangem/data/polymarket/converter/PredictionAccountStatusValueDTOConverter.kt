package com.tangem.data.polymarket.converter

import com.tangem.data.polymarket.entity.PredictionAccountStatusValueDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue

/**
 * Maps between the [PredictionAccountStatusValue] domain model and the [PredictionAccountStatusValueDTO]
 * storage DTO.
 *
 * [convert] returns `null` for the states that must not outlive the session — loading, and every error. A
 * persisted error would be indistinguishable from a freshly observed one on the next launch, and the account
 * would open on a failure that may have lasted a second.
 *
 * [convertBack] always restores the value as [StatusSource.CACHE]: whatever it was when it was written, it has
 * not been refreshed in this session, and the wallet total is flagged from this source.
 */
internal object PredictionAccountStatusValueDTOConverter {

    fun convert(value: PredictionAccountStatusValue): PredictionAccountStatusValueDTO? = when (value) {
        is PredictionAccountStatusValue.NotOnboarded -> PredictionAccountStatusValueDTO.NotOnboarded
        is PredictionAccountStatusValue.Onboarded -> PredictionAccountStatusValueDTO.Onboarded
        is PredictionAccountStatusValue.Onboarding -> PredictionAccountStatusValueDTO.Onboarding(
            stage = value.stage.toDTO(),
        )
        is PredictionAccountStatusValue.Active -> PredictionAccountStatusValueDTO.Active(
            balance = value.balance,
            isTradingAllowed = value.isTradingAllowed,
        )
        is PredictionAccountStatusValue.Loading,
        is PredictionAccountStatusValue.Error,
        -> null
    }

    fun convertBack(value: PredictionAccountStatusValueDTO): PredictionAccountStatusValue = when (value) {
        is PredictionAccountStatusValueDTO.NotOnboarded -> PredictionAccountStatusValue.NotOnboarded
        is PredictionAccountStatusValueDTO.Onboarded -> PredictionAccountStatusValue.Onboarded(
            source = StatusSource.CACHE,
        )
        is PredictionAccountStatusValueDTO.Onboarding -> PredictionAccountStatusValue.Onboarding(
            source = StatusSource.CACHE,
            stage = value.stage.toDomain(),
        )
        is PredictionAccountStatusValueDTO.Active -> PredictionAccountStatusValue.Active(
            source = StatusSource.CACHE,
            balance = value.balance,
            fiatRate = null,
            isTradingAllowed = value.isTradingAllowed,
        )
    }

    private fun PredictionAccountStatusValue.Onboarding.Stage.toDTO() = when (this) {
        PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING -> PredictionAccountStatusValueDTO.Stage.DEPLOYING
        PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED -> PredictionAccountStatusValueDTO.Stage.DEPLOYED
        PredictionAccountStatusValue.Onboarding.Stage.APPROVING -> PredictionAccountStatusValueDTO.Stage.APPROVING
    }

    private fun PredictionAccountStatusValueDTO.Stage.toDomain() = when (this) {
        PredictionAccountStatusValueDTO.Stage.DEPLOYING -> PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING
        PredictionAccountStatusValueDTO.Stage.DEPLOYED -> PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED
        PredictionAccountStatusValueDTO.Stage.APPROVING -> PredictionAccountStatusValue.Onboarding.Stage.APPROVING
    }
}