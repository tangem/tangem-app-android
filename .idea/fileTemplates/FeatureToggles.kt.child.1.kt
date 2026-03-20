#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}#end

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class Default${NAME}FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : ${NAME}FeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.)
}