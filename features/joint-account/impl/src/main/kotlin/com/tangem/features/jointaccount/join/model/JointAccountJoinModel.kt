package com.tangem.features.jointaccount.join.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class JointAccountJoinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val draftHolder = JointAccountJoinDraftHolder()
}