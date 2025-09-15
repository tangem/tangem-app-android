package com.tangem.common.routing.entity

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class SerializableIntent(
    val action: String?,
    val dataString: String?,
    val categories: Set<String>?,
    val type: String?,
    val packageValue: String?,
    val component: String?,
    val flags: Int,
    // CAUTION: works wrong with SerializableBundle constructor(bundle: Bundle), need to be removed
    val extras: SerializableBundle?,
) {

    constructor(intent: Intent) : this(
        action = intent.action,
        dataString = intent.dataString,
        categories = intent.categories,
        type = intent.type,
        packageValue = intent.`package`,
        component = intent.component?.flattenToString(),
        flags = intent.flags,
        extras = intent.extras?.let(::SerializableBundle),
    )

    fun toIntent(): Intent {
        val intent = Intent()

        intent.action = action
        intent.setDataAndType(
            dataString?.let { Uri.parse(it) },
            type,
        )
        categories?.let { categories ->
            for (category in categories) {
                intent.addCategory(category)
            }
        }
        intent.`package` = packageValue
        intent.component = component?.let { ComponentName.unflattenFromString(it) }
        intent.flags = flags
        extras?.let { intent.putExtras(it.toBundle()) }

        return intent
    }
}