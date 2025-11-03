#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.fetcher#end

import com.tangem.domain.core.flow.FlowFetcher

interface ${NAME}Fetcher : FlowFetcher<${NAME}Fetcher.Params> {

    data class Params(
        val id: String, // TODO("Not yet implemented")
    )
}