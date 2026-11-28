package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Device-local second-factor gate for the payroll screen.
 *
 * The verifier is an HMAC using a non-exportable Android Keystore key; the PIN itself is never
 * persisted. This is not a substitute for server-side role checks or server authorization.
 */
class PayrollPinManager(context: Context) {
  private val preferences =
    context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  @Synchronized
  fun hasPin(): Boolean =
    !preferences.getString(KEY_SALT, null).isNullOrBlank() &&
      !preferences.getString(KEY_VERIFIER, null).isNullOrBlank()

  @Synchronized
  fun setPin(pin: String): Boolean {
    if (!isValidPin(pin) || hasPin()) return false
    return runCatching {
      val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
      val verifier = calculateVerifier(getOrCreateKey(), salt, pin)
      preferences.edit()
        .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        .putString(KEY_VERIFIER, Base64.encodeToString(verifier, Base64.NO_WRAP))
        .putInt(KEY_FAILED_ATTEMPTS, 0)
        .putLong(KEY_LOCK_UNTIL, 0L)
        .commit()
    }.getOrDefault(false)
  }

  @Synchronized
  fun verifyPin(pin: String): Boolean {
    if (!isValidPin(pin) || !hasPin()) return false
    val now = System.currentTimeMillis()
    val lockUntil = preferences.getLong(KEY_LOCK_UNTIL, 0L)
    if (now < lockUntil) return false

    return runCatching {
      val salt = Base64.decode(preferences.getString(KEY_SALT, null), Base64.NO_WRAP)
      val expected = Base64.decode(preferences.getString(KEY_VERIFIER, null), Base64.NO_WRAP)
      val actual = calculateVerifier(getOrCreateKey(), salt, pin)
      val matches = MessageDigest.isEqual(expected, actual)
      if (matches) {
        preferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
      } else {
        val attempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = preferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        if (attempts >= MAX_FAILED_ATTEMPTS) {
          editor.putInt(KEY_FAILED_ATTEMPTS, 0)
          editor.putLong(KEY_LOCK_UNTIL, now + LOCKOUT_MS)
        }
        editor.apply()
      }
      matches
    }.getOrDefault(false)
  }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEY_STORE)
    generator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
      )
        .setDigests(KeyProperties.DIGEST_SHA256)
        .setUserAuthenticationRequired(false)
        .build()
    )
    return generator.generateKey()
  }

  private fun calculateVerifier(key: SecretKey, salt: ByteArray, pin: String): ByteArray {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    mac.init(key)
    mac.update(salt)
    return mac.doFinal(pin.toByteArray(Charsets.UTF_8))
  }

  companion object {
    private const val PREFERENCES_NAME = "officezen_payroll_pin_v1"
    private const val KEY_SALT = "salt"
    private const val KEY_VERIFIER = "verifier"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCK_UNTIL = "lock_until"
    private const val KEY_ALIAS = "officezen_payroll_pin_hmac_v1"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val SALT_BYTES = 16
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_MS = 60_000L

    fun isValidPin(pin: String): Boolean = pin.matches(Regex("^[0-9]{6}$"))
  }
}
