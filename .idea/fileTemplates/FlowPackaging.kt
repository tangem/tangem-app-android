#if ($FlowType && $FlowType != "")
    #set($Type = $FlowType)
#else
    #set($Type = "TODO")
#end
#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.producer#end

import com.tangem.domain.core.flow.FlowProducer

interface ${NAME}Producer : FlowProducer<${FlowType}> {

    data class Params(
        val id: String, // TODO("Not yet implemented")
    )

    interface Factory : FlowProducer.Factory<Params, ${NAME}Producer>
}