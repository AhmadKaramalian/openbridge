package com.openbridge.android.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

/**
 * Crypto utilities using Android Keystore (hardware-backed when available).
 *
 * All keys are generated in the Keystore and never extracted.
 * The private key never leaves the secure hardware.
 *
 * Algorithms:
 *  - ECDH P-256 for key agreement (BLE pairing)
 *  - ECDSA P-256 for signatures (command authentication)
 *  - AES-GCM (via Keystore) for symmetric encryption
 */
object CryptoUtil {

    private const val TAG = "CryptoUtil"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val EC_CURVE = "secp256r1"

    /**
     * Generate an ECDH keypair in the Keystore.
     * The private key is non-extractable.
     */
    fun generateEcdhKeyPair(alias: String): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_AGREE_KEY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setRandomizedEncryptionRequired(true)
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    /**
     * Generate an ECDSA signing keypair in the Keystore.
     */
    fun generateEcdsaKeyPair(alias: String): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setRandomizedEncryptionRequired(true)
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    /**
     * Perform ECDH key agreement to derive a shared secret.
     *
     * @param ourAlias The alias of our private key in the Keystore.
     * @param theirPublicKey The peer's public key.
     * @return 32-byte shared secret.
     */
    fun ecdh(ourAlias: String, theirPublicKey: PublicKey): ByteArray {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val ourPriv = ks.getKey(ourAlias, null) as PrivateKey

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(ourPriv)
        ka.doPhase(theirPublicKey, true)
        return ka.generateSecret()
    }

    /**
     * Sign data with ECDSA SHA-256 using a Keystore key.
     */
    fun sign(alias: String, data: ByteArray): ByteArray {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val priv = ks.getKey(alias, null) as PrivateKey

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(priv)
        sig.update(data)
        return sig.sign()
    }

    /**
     * Verify an ECDSA SHA-256 signature.
     */
    fun verify(publicKey: PublicKey, data: ByteArray, signature: ByteArray): Boolean {
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(publicKey)
        sig.update(data)
        return sig.verify(signature)
    }

    /**
     * Generate an ephemeral ECDH keypair OUTSIDE the Keystore (for one-time use).
     * Used during pairing before keys are stored.
     */
    fun generateEphemeralEcdhKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(EC_CURVE))
        return kpg.generateKeyPair()
    }

    /**
     * HKDF-SHA256 to derive multiple keys from a shared secret.
     *
     * @param ikm Input keying material (e.g., ECDH shared secret).
     * @param info Context string (e.g., "openbridge-v0-pairing").
     * @param length Output length in bytes.
     */
    fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        // Extract
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(salt.ifEmpty { ByteArray(32) }, "HmacSHA256"))
        val prk = mac.doFinal(ikm)

        // Expand
        val out = ByteArray(length)
        var t = ByteArray(0)
        var pos = 0
        var counter: Byte = 1
        while (pos < length) {
            mac.init(javax.crypto.spec.SecretKeySpec(prk, "HmacSHA256"))
            mac.update(t)
            mac.update(info)
            mac.update(counter)
            t = mac.doFinal()
            val take = minOf(t.size, length - pos)
            System.arraycopy(t, 0, out, pos, take)
            pos += take
            counter++
        }
        return out
    }

    /**
     * Check if a Keystore alias exists.
     */
    fun aliasExists(alias: String): Boolean {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return ks.containsAlias(alias)
    }

    /**
     * Delete a Keystore alias (e.g., on unpair).
     */
    fun deleteAlias(alias: String) {
        try {
            val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias)
                Log.i(TAG, "deleted keystore alias: $alias")
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to delete alias $alias: ${e.message}")
        }
    }
}
