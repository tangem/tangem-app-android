#if ($TOGGLE_NAME && $TOGGLE_NAME != "")
    #set($TOGGLE = $TOGGLE_NAME)
#else
    #set($TOGGLE = "TODO")
#end
#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

interface ${NAME}FeatureToggles {

    val isFeatureEnabled: Boolean
}