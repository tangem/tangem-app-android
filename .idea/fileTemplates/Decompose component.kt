#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}#end

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ${NAME}Component : ComposableContentComponent {

    data class Params(
        val data: Any, // TODO("Not yet implemented")
    )
    
    interface Factory : ComponentFactory<Params, ${NAME}Component>
}