package com.tangem.tangem_sdk_new

import android.app.Application
import android.content.SharedPreferences
import at.favre.lib.armadillo.Armadillo
import com.tangem.KeyPair
import com.tangem.common.TerminalKeysService
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils


/**
 * Service for managing Terminal keypair, used for Linked Terminal feature.
 * Needs to be provided to [com.tangem.TangemSdk] by calling [com.tangem.TangemSdk.setTerminalKeysService]
 * Linked Terminal feature can be disabled manually by editing [com.tangem.Config].
 * @param applicationContext is required to retrieve an instance of [SharedPreferences]
 */
class TerminalKeysStorage(applicationContext: Application): TerminalKeysService {

    private val preferences: SharedPreferences by lazy {
        Armadillo.create(applicationContext, PREFERENCES_KEY)
                .encryptionFingerprint(applicationContext)
                .build()
    }

    /**
     * Retrieves generated keys from encrypted shared preferences if the keys exist.
     * Generates new and stores them in encrypted shared preferences otherwise
     */
    override fun getKeys(): KeyPair {
        val privateKey = preferences.getString(TERMINAL_PRIVATE_KEY, null)?.hexToBytes()
        val publicKey = preferences.getString(TERMINAL_PUBLIC_KEY, null)?.hexToBytes()
        return if (privateKey == null || publicKey == null) {
            generateAndSaveKeys()
        } else {
            KeyPair(publicKey, privateKey)
        }
    }

    private fun generateAndSaveKeys(): KeyPair {
        val privateKey = CryptoUtils.generateRandomBytes(32)
        val publicKey = CryptoUtils.generatePublicKey(privateKey)
        preferences.edit().putString(TERMINAL_PRIVATE_KEY, privateKey.toHexString()).apply()
        preferences.edit().putString(TERMINAL_PUBLIC_KEY, publicKey.toHexString()).apply()
        return KeyPair(publicKey, privateKey)
    }


    companion object {
        const val PREFERENCES_KEY = "myPrefs"
        const val TERMINAL_PRIVATE_KEY = "terminalPrivateKey"
        const val TERMINAL_PUBLIC_KEY = "terminalPublicKey"
    }

}