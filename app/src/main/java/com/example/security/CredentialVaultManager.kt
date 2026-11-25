package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@JsonClass(generateAdapter = true)
data class VaultCredential(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val pressEnterAfter: Boolean = true,
)

/**
 * Hardware-backed Android Keystore AES-256-GCM encrypted Credential Vault.
 * Passwords are encrypted at rest and only decrypted in memory after Biometric authentication
 * to auto-type (Username + Tab + Password + Enter) via Bluetooth HID.
 */
class CredentialVaultManager(private val context: Context) {

    private val keyAlias = "controle_bt_vault_aes_key"
    private val vaultFile: File get() = File(context.filesDir, "vault_credentials.enc")

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, VaultCredential::class.java)
    private val adapter = moshi.adapter<List<VaultCredential>>(listType)

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    fun loadCredentials(): List<VaultCredential> {
        if (!vaultFile.exists()) return emptyList()
        return runCatching {
            val encoded = vaultFile.readText().trim()
            val parts = encoded.split(":")
            if (parts.size != 2) return emptyList()
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val json = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            adapter.fromJson(json).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun saveCredentials(items: List<VaultCredential>) {
        runCatching {
            val json = adapter.toJson(items)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            vaultFile.writeText("$ivB64:$dataB64")
        }
    }

    fun addOrUpdate(cred: VaultCredential): List<VaultCredential> {
        val current = loadCredentials().toMutableList()
        val idx = current.indexOfFirst { it.id == cred.id }
        if (idx >= 0) current[idx] = cred else current.add(0, cred)
        saveCredentials(current)
        return current
    }

    fun delete(id: String): List<VaultCredential> {
        val next = loadCredentials().filterNot { it.id == id }
        saveCredentials(next)
        return next
    }
}
