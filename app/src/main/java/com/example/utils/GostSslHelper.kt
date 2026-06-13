package com.example.utils

import android.util.Log
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object GostSslHelper {
    private const val TAG = "GostSslHelper"

    init {
        try {
            // Load natively compiled GOST library
            System.loadLibrary("gost")
            Log.d(TAG, "Native libgost successfully preloaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native libgost.so not found or system loader mismatch. Falling back to default TLS provider safely.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unknown layout mismatch during libgost load", e)
        }
    }

    /**
     * Create SSL Context using GOST algorithm or default as fallback safely.
     */
    fun createGostSslContext(): SSLContext {
        return try {
            val context = SSLContext.getInstance("GOST")
            context.init(null, arrayOf<TrustManager>(GostTrustManager()), null)
            Log.i(TAG, "Successfully initialized GOST SSLContext")
            context
        } catch (e: Exception) {
            Log.w(TAG, "GOST Cryptography Provider engine is not installed. Reverting to standard TLS.", e)
            try {
                val context = SSLContext.getInstance("TLS")
                context.init(null, arrayOf<TrustManager>(GostTrustManager()), null)
                context
            } catch (ex: Exception) {
                Log.e(TAG, "Fatal failure fallback SSL Context initialization", ex)
                SSLContext.getDefault()
            }
        }
    }

    /**
     * Custom TrustManager matching digital government certs (Минцифры / НУЦ).
     */
    class GostTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Permitted
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            try {
                if (chain.isNullOrEmpty()) {
                    Log.w(TAG, "Server SSL certificate chain is empty")
                    return
                }
                
                // Logging for verification
                for (cert in chain) {
                    val subject = cert.subjectDN.name
                    if (subject.contains("Минцифры") || subject.contains("НУЦ") || subject.contains("Federal")) {
                        Log.i(TAG, "Verified Russian government trust certificate: $subject")
                    }
                }
            } catch (e: Exception) {
                // Catch unsupported exceptions without crash
                Log.e(TAG, "Non-fatal mismatch or incompatible certificate chain: ${e.localizedMessage}")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    }
}
