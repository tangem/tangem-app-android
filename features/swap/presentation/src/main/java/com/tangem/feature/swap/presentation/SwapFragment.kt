package com.tangem.feature.swap.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.feature.swap.domain.SwapInteractorImpl
import com.tangem.feature.swap.domain.SwapRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SwapFragment : Fragment() {

    @Inject
    lateinit var swapRepository: SwapRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //test
        val swapInteractor = SwapInteractorImpl(swapRepository)

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("test", swapInteractor.getTokensToSwap("ethereum").toString())
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}