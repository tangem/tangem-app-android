package com.tangem.features.survey.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.survey.SurveyFeatureToggles

internal class DefaultSurveyFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : SurveyFeatureToggles {

    override val isNpsSurveyEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1898_NPS_SURVEY_ENABLED)
}