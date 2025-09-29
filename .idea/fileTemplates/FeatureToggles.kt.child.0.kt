#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class Default${NAME}FeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : ${NAME}FeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = TODO("add feature name"))
}