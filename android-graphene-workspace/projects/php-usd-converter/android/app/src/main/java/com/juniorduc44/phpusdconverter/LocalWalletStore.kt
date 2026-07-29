package com.juniorduc44.phpusdconverter

import android.content.Context
import android.util.Base64
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.experimental.xor

/**
 * Password-encrypted local EOA keystore (SharedPreferences).
 * Self-custody only — not Robinhood login / not passkey AA yet.
 */
class LocalWalletStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var unlockedPk: String? = null

    val hasWallet: Boolean
        get() = !prefs.getString(KEY_ADDRESS, null).isNullOrBlank() &&
            !prefs.getString(KEY_CIPHER, null).isNullOrBlank()

    val address: String?
        get() = prefs.getString(KEY_ADDRESS, null)

    val isUnlocked: Boolean
        get() = unlockedPk != null

    fun create(password: String): String {
        require(password.length >= 6) { "Password min 6 characters" }
        val keys = Keys.createEcKeyPair()
        val creds = Credentials.create(keys)
        val pk = Numeric.toHexStringWithPrefixZeroPadded(keys.privateKey, 64)
        val addr = Keys.toChecksumAddress(creds.address)
        saveEncrypted(addr, pk, password)
        unlockedPk = pk
        return addr
    }

    fun unlock(password: String): String {
        require(hasWallet) { "No wallet" }
        val salt = prefs.getString(KEY_SALT, null) ?: throw IllegalStateException("corrupt")
        val cipher = prefs.getString(KEY_CIPHER, null) ?: throw IllegalStateException("corrupt")
        val pk = decrypt(cipher, salt, password)
        val creds = Credentials.create(pk)
        val stored = address?.lowercase() ?: ""
        if (creds.address.lowercase() != stored &&
            Keys.toChecksumAddress(creds.address).lowercase() != stored
        ) {
            // eth addresses may differ in checksum; compare without 0x case
            if (Numeric.cleanHexPrefix(creds.address).lowercase() !=
                Numeric.cleanHexPrefix(stored).lowercase()
            ) {
                throw IllegalArgumentException("Wrong password")
            }
        }
        unlockedPk = Numeric.toHexStringWithPrefix(Numeric.toBigInt(pk))
        return address ?: creds.address
    }

    fun lock() {
        unlockedPk = null
    }

    fun delete() {
        lock()
        prefs.edit().clear().apply()
    }

    private fun saveEncrypted(address: String, privateKey: String, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val cipherB64 = encrypt(privateKey, saltB64, password)
        prefs.edit()
            .putString(KEY_ADDRESS, address)
            .putString(KEY_SALT, saltB64)
            .putString(KEY_CIPHER, cipherB64)
            .putInt(KEY_CHAIN, 4663)
            .apply()
    }

    private fun derive(password: String, saltB64: String): ByteArray {
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    private fun encrypt(plain: String, saltB64: String, password: String): String {
        val key = derive(password, saltB64)
        val data = plain.toByteArray(Charsets.UTF_8)
        val out = ByteArray(data.size)
        var counter = 0
        var block = ByteArray(0)
        for (i in data.indices) {
            if (i % 32 == 0) {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                md.update(key)
                md.update(byteArrayOf(
                    (counter shr 24).toByte(),
                    (counter shr 16).toByte(),
                    (counter shr 8).toByte(),
                    counter.toByte(),
                ))
                block = md.digest()
                counter++
            }
            out[i] = data[i] xor block[i % 32]
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(cipherB64: String, saltB64: String, password: String): String {
        val key = derive(password, saltB64)
        val data = Base64.decode(cipherB64, Base64.NO_WRAP)
        val out = ByteArray(data.size)
        var counter = 0
        var block = ByteArray(0)
        for (i in data.indices) {
            if (i % 32 == 0) {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                md.update(key)
                md.update(byteArrayOf(
                    (counter shr 24).toByte(),
                    (counter shr 16).toByte(),
                    (counter shr 8).toByte(),
                    counter.toByte(),
                ))
                block = md.digest()
                counter++
            }
            out[i] = data[i] xor block[i % 32]
        }
        val text = out.toString(Charsets.UTF_8)
        if (!text.startsWith("0x") && text.length < 64) throw IllegalArgumentException("bad decrypt")
        return if (text.startsWith("0x")) text else "0x$text"
    }

    companion object {
        private const val PREFS = "wallet_keystore"
        private const val KEY_ADDRESS = "address"
        private const val KEY_SALT = "salt"
        private const val KEY_CIPHER = "cipher"
        private const val KEY_CHAIN = "chain_id"
    }
}
