package com.tangem.tap.features.send.ui

import androidx.lifecycle.*
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Send screen view model
 *
 * @property dispatchers                        coroutine dispatchers provider
 * @property appStateHolder                     redux state holder
 *
 * @author Andrew Khokhlov on 15/06/2023
 */
@HiltViewModel
internal class SendViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val appStateHolder: AppStateHolder,
    private val isBalanceHiddenUseCase: IsBalanceHiddenUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
) : ViewModel(), DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        isBalanceHiddenUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .flowOn(dispatchers.io)
            .onEach { isBalanceHidden ->
                withContext(dispatchers.main) {
                    appStateHolder.mainStore?.dispatch(AmountAction.HideBalance(isBalanceHidden))
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            listenToFlipsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .flowOn(dispatchers.io)
                .collect()
        }
    }
}
