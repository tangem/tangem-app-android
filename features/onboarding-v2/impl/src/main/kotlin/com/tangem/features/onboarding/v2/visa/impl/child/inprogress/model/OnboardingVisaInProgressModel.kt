package com.tangem.features.onboarding.v2.visa.impl.child.inprogress.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaInProgressModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val onDone = MutableSharedFlow<Unit>()

    init {
        modelScope.launch {
            // TODO check state
            delay(timeMillis = 2000)
            onDone.emit(Unit)
        }
    }
}
