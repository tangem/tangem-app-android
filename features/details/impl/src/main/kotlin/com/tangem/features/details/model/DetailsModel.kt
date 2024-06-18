package com.tangem.features.details.model

import com.tangem.core.decompose.model.Model
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject
// [REDACTED_TODO_COMMENT]
internal class DetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model()
