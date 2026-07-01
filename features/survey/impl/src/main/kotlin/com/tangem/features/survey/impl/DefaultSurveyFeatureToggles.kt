package com.tangem.features.survey.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.survey.SurveyFeatureToggles
import javax.inject.Inject

internal class DefaultSurveyFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SurveyFeatureToggles {

    override val areSurveysEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15482_SURVEYSPARROW_ENABLED)
}