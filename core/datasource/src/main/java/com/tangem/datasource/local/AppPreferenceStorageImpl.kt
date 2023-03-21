package com.tangem.datasource.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.autsoft.krate.SimpleKrate
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.stringPref
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of application local storage
 *
 * @param context application context
 *
 * @author Andrew Khokhlov on 08/02/2023
 */
@Singleton
internal class AppPreferenceStorageImpl @Inject constructor(
    @ApplicationContext context: Context,
) : SimpleKrate(context = context), AppPreferenceStorage {

    override var featureToggles: String by stringPref().withDefault("")
}
