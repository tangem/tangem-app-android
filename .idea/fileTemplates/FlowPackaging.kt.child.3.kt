#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.fetcher#end

import arrow.core.Either
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class Default${NAME}Fetcher @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
) : ${NAME}Fetcher {
    
    override suspend fun invoke(params: ${NAME}Fetcher.Params): Either<Throwable, Unit> {
        TODO("Not yet implemented")
    }
}