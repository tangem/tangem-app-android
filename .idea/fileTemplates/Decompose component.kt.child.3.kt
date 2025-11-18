#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}#end

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import ${PACKAGE_NAME}.model.${NAME}Model
import ${PACKAGE_NAME}.ui.${NAME}Content
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class Default${NAME}Component @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ${NAME}Component.Params,
) : AppComponentContext by appComponentContext, ${NAME}Component {

    private val model: ${NAME}Model = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        
        ${NAME}Content(modifier = modifier, state = state)
        
        BackHandler(onBack = state.onBackClick)
    }

    @AssistedFactory
    interface Factory : ${NAME}Component.Factory {
        override fun create(
            context: AppComponentContext,
            params: ${NAME}Component.Params,
        ): Default${NAME}Component
    }
}