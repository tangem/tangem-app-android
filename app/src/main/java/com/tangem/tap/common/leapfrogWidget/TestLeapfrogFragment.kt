package com.tangem.tap.common.leapfrogWidget

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidgetState
import com.tangem.tap.domain.twins.TwinsCardWidget
import com.tangem.wallet.R
import com.tangem.wallet.databinding.TestLeapfrogFragmentBinding

class TestLeapfrogFragment : Fragment(R.layout.test_leapfrog_fragment) {

    private lateinit var twinsCardWidget: TwinsCardWidget
    private val binding: TestLeapfrogFragmentBinding by viewBinding(TestLeapfrogFragmentBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    @Suppress("MagicNumber")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val leapfrogContainer: FrameLayout = view.findViewById(R.id.leapfrog_views_container)
        val leapfrog = LeapfrogWidget(leapfrogContainer)
        twinsCardWidget = TwinsCardWidget(leapfrog) { 200f }

        binding.btnTwinWelcome.setOnClickListener {
            twinsCardWidget.toWelcome()
        }
        binding.btnTwinToLeapfrog.setOnClickListener {
            twinsCardWidget.toLeapfrog()
        }
        binding.btnTwinActivate.setOnClickListener {
            twinsCardWidget.toActivate()
        }

        binding.btnLpInit.setOnClickListener {
            leapfrog.initViews()
        }
        binding.btnLpUnfold.setOnClickListener {
            leapfrog.unfold()
        }
        binding.btnLpFold.setOnClickListener {
            leapfrog.fold()
        }
        binding.btnLpLeap.setOnClickListener {
            leapfrog.leap()
        }
        binding.btnLpLeapBack.setOnClickListener {
            leapfrog.leapBack()
        }
    }

    override fun onStart() {
        super.onStart()
        leapfrogWidgetState?.let { twinsCardWidget.leapfrogWidget.applyState(it) }
    }

    override fun onStop() {
        super.onStop()
        leapfrogWidgetState = twinsCardWidget.leapfrogWidget.getState()
    }
}

private var leapfrogWidgetState: LeapfrogWidgetState? = null
