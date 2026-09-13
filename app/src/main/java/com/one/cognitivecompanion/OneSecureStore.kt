package com.one.cognitivecompanion

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

data class StoredOneSession(
    val session: OneSession,
    val onboardingComplete: Boolean
)

/** JSON envelope kept separate from the Android Keystore wrapper. */
internal object OneSessionEnvelopeCodec {
    fun encode(stored: StoredOneSession): String {
        val session = stored.session
        return JSONObject()
            .put("access_token", session.accessToken)
            .put("home_id", session.homeId.toString())
            .put("user_id", session.userId.toString())
            .put("role", session.role.wireValue)
            .put("backend_role", session.backendRole)
            .put("expires_at", session.expiresAt?.epochSecond ?: JSONObject.NULL)
            .put("onboarding_complete", stored.onboardingComplete)
            .toString()
    }

    fun decode(raw: String): StoredOneSession? = runCatching {
        val body = JSONObject(raw)
        val accessToken = body.optString("access_token").takeIf { it.isNotBlank() }
            ?: return@runCatching null
        val homeId = UUID.fromString(body.optString("home_id"))
        val userId = UUID.fromString(body.optString("user_id"))
        val expiresAt = if (body.isNull("expires_at")) {
            null
        } else {
            Instant.ofEpochSecond(body.getLong("expires_at"))
        }
        val role = body.optString("role").toOneRole()
        val backendRole = body.optString("backend_role").takeIf { it.isNotBlank() } ?: role.wireValue
        StoredOneSession(
            session = OneSession(
                accessToken = accessToken,
                homeId = homeId,
                userId = userId,
                role = role,
                expiresAt = expiresAt,
                backendRole = backendRole
            ),
            onboardingComplete = body.optBoolean("onboarding_complete", false)
        )
    }.getOrNull()
}

/**
 * Stores the bearer session in an AES/GCM envelope. The AES key is generated
 * and retained by Android Keystore, so the token is not readable from the
 * app's ordinary preferences or backup data.
 */
class OneSecureStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveSession(session: OneSession, onboardingComplete: Boolean = false) {
        val encrypted = encrypt(OneSessionEnvelopeCodec.encode(StoredOneSession(session, onboardingComplete)))
        check(preferences.edit().putString(SESSION_KEY, encrypted).commit()) { "Could not persist the ONE session." }
    }

    fun markOnboardingComplete(session: OneSession) {
        saveSession(session, onboardingComplete = true)
    }

    fun restore(): StoredOneSession? {
        val encrypted = preferences.getString(SESSION_KEY, null) ?: return null
        val restored = runCatching { OneSessionEnvelopeCodec.decode(decrypt(encrypted)) }.getOrNull()
        if (restored == null || restored.session.expiresAt?.isBefore(Instant.now()) == true) {
            clear()
            return null
        }
        return restored
    }

    fun clear() {
        check(preferences.edit().remove(SESSION_KEY).commit()) { "Could not clear the ONE session." }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val initializationVector = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = byteArrayOf(initializationVector.size.toByte()) + initializationVector + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > 1) { "Invalid encrypted ONE session." }
        val ivSize = payload[0].toInt() and 0xFF
        require(ivSize in 12..16 && payload.size > ivSize + 1) { "Invalid encrypted ONE session." }
        val initializationVector = payload.copyOfRange(1, ivSize + 1)
        val encrypted = payload.copyOfRange(ivSize + 1, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "one.session.aes"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFERENCES_NAME = "one.secure.session"
        const val SESSION_KEY = "encrypted_session"
    }
}
