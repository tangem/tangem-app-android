package com.tangem.devkit.ucase.domain.paramsManager.managers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.gson.Gson
import com.tangem.TangemSdk
import com.tangem.devkit.commons.Store
import com.tangem.devkit.ucase.domain.actions.PersonalizeAction
import com.tangem.devkit.ucase.domain.paramsManager.ActionCallback
import com.tangem.devkit.ucase.variants.personalize.converter.PersonalizationConfigConverter
import com.tangem.devkit.ucase.variants.personalize.converter.PersonalizationJsonConverter
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationConfig
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationJson
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizationItemsManager(
        private val store: Store<PersonalizationConfig>
) : BaseItemsManager(PersonalizeAction()) {

    private val converter = PersonalizationConfigConverter()

    init {
        val config = store.restore()
        setItems(converter.convert(config))
    }

    override fun invokeMainAction(tangemSdk: TangemSdk, callback: ActionCallback) {
        action.executeMainAction(this, getAttrsForAction(tangemSdk), callback)
    }

    fun importJsonConfig(jsonString: String) {
        if (jsonString.isEmpty()) return

        val jsonDto = try {
            Gson().fromJson(jsonString, PersonalizationJson::class.java)
        } catch (ex: Exception) {
            Log.e(this, "Can't convert imported string to Json object. Error: $ex")
            return
        }

        val config = PersonalizationJsonConverter().aToB(jsonDto)
        setItems(converter.convert(config))
    }

    fun exportJsonConfig(): String {
        val config = converter.convert(itemList, PersonalizationConfig.default())
        val jsonDto = PersonalizationJsonConverter().bToA(config)
        val jsonString = Gson().toJson(jsonDto)
        return jsonString
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        val config = converter.convert(itemList, PersonalizationConfig.default())
        store.save(config)
    }
}