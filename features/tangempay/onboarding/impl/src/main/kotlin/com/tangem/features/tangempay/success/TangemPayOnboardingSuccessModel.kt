package com.tangem.features.tangempay.success

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class TangemPayOnboardingSuccessModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    fun onCloseClick() {
        router.pop()
    }
}