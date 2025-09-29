#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

interface ${NAME}FeatureToggles {

    val isFeatureEnabled: Boolean
}