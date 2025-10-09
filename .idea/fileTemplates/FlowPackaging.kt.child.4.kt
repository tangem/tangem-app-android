#if ($FlowType && $FlowType != "")
    #set($Type = $FlowType)
#else
    #set($Type = "TODO")
#end
#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.producer#end

import arrow.core.Option
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class Default${NAME}Producer @AssistedInject constructor(
    @Assisted val params: ${NAME}Producer.Params,
    private val dispatchers: CoroutineDispatcherProvider,
) : TestProducer {

    override val fallback: Option<$Type>
        get() = TODO("Not yet implemented")

    override fun produce(): Flow<$Type> {
        TODO("Not yet implemented")
    }

    @AssistedFactory
    interface Factory : ${NAME}Producer.Factory {
        override fun create(params: ${NAME}Producer.Params): Default${NAME}Producer
    }
}