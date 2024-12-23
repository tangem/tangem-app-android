package com.tangem.tap

import android.os.Build
import android.view.MotionEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.TechAnalyticsEvent
import com.tangem.core.analytics.models.event.TechAnalyticsEvent.WindowObscured.ObscuredState
import timber.log.Timber

internal object WindowObscurationObserver : DefaultLifecycleObserver {

    private var isWindowPartiallyObscuredAlreadySent: Boolean = false
    private var isWindowFullyObscuredAlreadySent: Boolean = false

    private var isReadyToProxy = false

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isReadyToProxy = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isReadyToProxy = false
    }

    fun dispatchTouchEvent(event: MotionEvent, analyticsEventHandler: AnalyticsEventHandler): Boolean {
        if (!isReadyToProxy) return true

        val isPartiallyObscured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
        } else {
            false
        }

        if (isPartiallyObscured) {
            Timber.d("Window is partially obscured")

            if (!isWindowPartiallyObscuredAlreadySent) {
                analyticsEventHandler.send(
                    event = TechAnalyticsEvent.WindowObscured(state = ObscuredState.PARTIALLY),
                )

                isWindowPartiallyObscuredAlreadySent = true
            }
        }

        val isFullyObscured = event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0

        if (isFullyObscured) {
            Timber.d("Window is partially or fully obscured")

            if (!isWindowFullyObscuredAlreadySent) {
                analyticsEventHandler.send(
                    event = TechAnalyticsEvent.WindowObscured(state = ObscuredState.FULLY),
                )

                isWindowFullyObscuredAlreadySent = true
            }

            return false
        }

        return true
    }
}