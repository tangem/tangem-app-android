package com.tangem.core.decompose.utils

import androidx.appcompat.app.AppCompatActivity
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import java.lang.ref.WeakReference

class ActivityInstanceHolder<T : Any> {
    private var instance: T? = null

    lateinit var instanceAccess: WeakReference<T>
        private set

    fun set(instance: T) {
        this.instance = instance
        instanceAccess = WeakReference(instance)
    }

    fun clear() {
        instance = null
        instanceAccess.clear()
    }
}

inline fun <reified T : Any> AppComponentContext.getOrCreateActivityInstanceHolder(
    noinline factory: (AppCompatActivity) -> T,
): ActivityInstanceHolder<T> {
    val holder = instanceKeeper.getOrCreateSimple {
        ActivityInstanceHolder<T>()
    }

    lifecycle.subscribe(
        onCreate = {
            holder.set(factory(activity))
        },
        onDestroy = {
            holder.clear()
        },
    )

    return holder
}