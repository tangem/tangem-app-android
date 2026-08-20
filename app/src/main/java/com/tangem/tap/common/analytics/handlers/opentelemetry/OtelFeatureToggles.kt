package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

interface OtelFeatureToggles {
    val isMetricsEnabled: Boolean
}

internal class DefaultOtelFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : OtelFeatureToggles {

    override val isMetricsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16115_OTEL_METRICS_ENABLED)
}