package com.tangem.features.disclaimer.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.features.disclaimer.api.components.DisclaimerComponent
import com.tangem.features.disclaimer.impl.entity.DisclaimerUM
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
@Suppress("LongParameterList")
internal class DisclaimerModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val cardRepository: CardRepository,
    private val router: Router,
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val appFinisher: AppFinisher,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: DisclaimerComponent.Params = paramsContainer.require()

    val state: MutableStateFlow<DisclaimerUM> = MutableStateFlow(
        DisclaimerUM(
            onAccept = ::onAccept,
            url = DISCLAIMER_URL,
            isTosAccepted = params.isTosAccepted,
            popBack = ::popBack,
        ),
    )

    private fun onAccept(shouldAskPushPermission: Boolean) = modelScope.launch {
        if (params.isTosAccepted) {
            router.pop()
        } else {
            cardRepository.acceptTangemTOS()

            if (shouldAskPushPermission) {
                router.push(AppRoute.PushNotification)
            } else {
                neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
                neverRequestPermissionUseCase(PUSH_PERMISSION)
                router.replaceAll(AppRoute.Home)
            }
        }
    }

    private fun popBack() {
        if (params.isTosAccepted) {
            router.pop()
        } else {
            appFinisher.finish()
        }
    }

    private companion object {
        const val DISCLAIMER_URL = "https://tangem.com/tangem_tos.html"
    }
}
