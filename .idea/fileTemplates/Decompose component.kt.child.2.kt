#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.model#end

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import ${PACKAGE_NAME}.${NAME}Component
import ${PACKAGE_NAME}.entity.${NAME}UM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class ${NAME}Model @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<${NAME}Component.Params>()

    val uiState: StateFlow<${NAME}UM>
        field: MutableStateFlow<${NAME}UM> = MutableStateFlow(getInitialState())

    private fun getInitialState(): ${NAME}UM {
        TODO("Not yet implemented")
    }
}