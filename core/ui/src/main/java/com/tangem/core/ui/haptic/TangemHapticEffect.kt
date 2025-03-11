package com.tangem.core.ui.haptic

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi

sealed interface TangemHapticEffect {

    /**
     * For cases when view could not be visible on screen (ex. Activity is not in foreground)
     * or there is no view context (ex. background service, Model, ViewModel)
     */
    enum class OneTime : TangemHapticEffect {
        Click,
        DoubleClick,
        HeavyClick,
        Tick,
        ;

        val code: Int
            @RequiresApi(Build.VERSION_CODES.Q)
            get() = when (this) {
                Click -> VibrationEffect.EFFECT_CLICK
                DoubleClick -> VibrationEffect.EFFECT_DOUBLE_CLICK
                HeavyClick -> VibrationEffect.EFFECT_HEAVY_CLICK
                Tick -> VibrationEffect.EFFECT_TICK
            }
    }

    /**
     * Preferred way to provide haptic feedback for UI components
     * @see [androidx.core.view.HapticFeedbackConstantsCompat]
     */
    @Suppress("MagicNumber")
    enum class View(internal val androidHapticFeedbackCode: Int? = null) : TangemHapticEffect {
        /**
         * The user has performed a long press on an object that is resulting in an action being
         * performed
         */
        LongPress(0),
        /**
         * The user has pressed on a virtual on-screen key
         */
        VirtualKey(1),
        /**
         * The user has pressed either an hour or minute tick of a Clock
         */
        ClockTick(4),
        /**
         * The user has performed a context click on an object
         */
        ContextClick(6),
        /**
         * The user has pressed a virtual or software keyboard key
         */
        KeyboardPress(3),
        /**
         * The user has released a virtual keyboard key
         */
        KeyboardRelease(7),
        /**
         * The user has released a virtual key
         */
        VirtualKeyRelease(8),
        /**
         * The user has performed a selection/insertion handle move on text field
         */
        TextHandleMove(9),
        /**
         * The user has started a gesture (e.g. on the soft keyboard)
         */
        GestureStart(12),
        /**
         * The user has finished a gesture (e.g. on the soft keyboard)
         */
        GestureEnd(13),
        /**
         * A haptic effect to signal the confirmation or successful completion of a user interaction
         */
        Confirm(16),
        /**
         * A haptic effect to signal the rejection or failure of a user interaction
         */
        Reject(17),
        /**
         * The user has toggled a switch or button into the on position
         */
        ToggleOn(21),
        /**
         * The user has toggled a switch or button into the off position
         */
        ToggleOff(22),
        /**
         * The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the
         * gesture action is “eligible” at a certain threshold of movement, and can be cancelled by
         * moving back past the threshold. This constant indicates that the user's motion has just
         * passed the threshold for the action to be activated on release
         */
        GestureThresholdActivate(23),
        /**
         * The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the
         * gesture action is “eligible” at a certain threshold of movement, and can be cancelled by
         * moving back past the threshold. This constant indicates that the user's motion has just
         * re-crossed back "under" the threshold for the action to be activated, meaning the gesture is
         * currently in a cancelled state
         */
        GestureThresholdDeactivate(24),
        /**
         * The user has started a drag-and-drop gesture. The drag target has just been "picked up"
         */
        DragStart(25),
        /**
         * The user is switching between a series of potential choices, for example items in a list
         * or discrete points on a slider
         */
        SegmentTick(26),
        /**
         * The user is switching between a series of many potential choices, for example minutes on a
         * clock face, or individual percentages. This constant is expected to be very soft, so as
         * not to be uncomfortable when performed a lot in quick succession. If the device can’t make
         * a suitably soft vibration, then it may not make any vibration
         */
        SegmentFrequentTick(27),
    }
}