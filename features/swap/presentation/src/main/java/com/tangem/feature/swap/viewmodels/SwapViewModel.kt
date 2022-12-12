package com.tangem.feature.swap.viewmodels

import androidx.lifecycle.ViewModel
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class SwapViewModel @Inject constructor(
    private val referralInteractor: SwapInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

}
