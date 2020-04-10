package com.tangem.tangemtest.commons.view

import android.widget.TextView
import androidx.annotation.StringRes
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.SafeValueChanged
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
open class MultiActionView<V : TextView>(
        stateList: MutableList<ButtonState>,
        val child: V
) {

    interface State {
        val id: Id

        fun getAction(): StateButtonAction
        fun getResNameId(): Int
    }

    var afterAction: SafeValueChanged<Id>? = null

    var state: Id = DefaultId.default
        set(value) {
            if (field == value) return

            field = value
            Log.d(this, "state changed to: ${getKey(value)}")
            btnState = stateHolder[getKey(value)]
        }

    init {
        child.setOnClickListener {
            val state = btnState ?: return@setOnClickListener

            Log.d(this, "child handled OnClick for: ${getKey(state.id)}")
            state.getAction().invoke()
            afterAction?.invoke(state.id)
        }
    }

    protected var btnState: State? = null
        set(value) {
            if (value == null) return

            field = value
            Log.d(this, "btnState changed to: ${getKey(value.id)}")
            child.setText(value.getResNameId())
        }

    protected val stateHolder: MutableMap<String, State> = stateList.associateBy { getKey(it.id) }.toMutableMap()

    fun performAction(id: Id) {
        Log.d(this, "performAction ${getKey(id)}")
        state = id
        child.performClick()
    }

    protected open fun getKey(id: Id): String {
        return when (id) {
            is StringId -> id.value
            else -> stringOf(id)
        }
    }
}

enum class DefaultId : Id { default }

typealias StateButtonAction = () -> Unit

class ButtonState(
        override val id: Id,
        @StringRes val name: Int,
        private val action: StateButtonAction
) : MultiActionView.State {

    override fun getAction(): StateButtonAction = action

    override fun getResNameId(): Int = name
}