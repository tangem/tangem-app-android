#if ($FlowType && $FlowType != "")
    #set($Type = $FlowType)
#else
    #set($Type = "TODO")
#end
#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.supplier#end

import ${PACKAGE_NAME}.producer.${NAME}Producer
import com.tangem.domain.core.flow.FlowCachingSupplier
import kotlinx.coroutines.flow.Flow

abstract class ${NAME}Supplier(
    override val factory: ${NAME}Producer.Factory,
    override val keyCreator: (${NAME}Producer.Params) -> String,
) : FlowCachingSupplier<${NAME}Producer, ${NAME}Producer.Params, $Type>()