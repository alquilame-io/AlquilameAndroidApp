package com.io.alquilame.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.io.alquilame.R
import com.io.alquilame.BuildConfig
import com.io.alquilame.MainActivity
import java.security.MessageDigest

class QRScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_RESULT = "qr_result"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_API_KEY = "api_key"
        const val EXTRA_AUTH_URL = "auth_url"
        const val EXTRA_IS_NONCE_AUTH = "is_nonce_auth"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start QR scanner immediately
        startQRScanner()
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR code to login")
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        
        if (result != null) {
            if (result.contents == null) {
                // User cancelled
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                // Process QR code result
                processQRResult(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun processQRResult(qrContent: String) {
        try {
            // Check if it's a nonce-based authentication URL (NEW METHOD)
            if (qrContent.startsWith("http://") || qrContent.startsWith("https://")) {
                // New QR code format: Contains full authentication URL with nonce
                // Example: https://negocio.alquilame.io/auth/nonce/negocio/Kx7pQmR9...
                if (qrContent.contains("/auth/nonce/")) {
                    processNonceAuthQR(qrContent)
                } else {
                    showError("Invalid authentication URL. Please scan a valid Alquilame QR code.")
                }
            } else {
                // Legacy QR code format: "tenant_slug|tenant_secret_slug|hash"
                processLegacyQR(qrContent)
            }
        } catch (e: Exception) {
            showError("Error processing QR code: ${e.message}")
        }
    }

    /**
     * NEW METHOD: Process nonce-based QR code
     * QR contains the complete auth URL with nonce
     */
    private fun processNonceAuthQR(authUrl: String) {
        // Extract tenant slug from URL for saving credentials
        // Example URL: https://negocio.alquilame.io/auth/nonce/negocio/Kx7pQmR9...
        val tenantSlug = extractTenantSlugFromUrl(authUrl)

        if (tenantSlug != null) {
            // Return the auth URL - MainActivity will handle opening it
            val resultIntent = Intent().apply {
                putExtra(EXTRA_AUTH_URL, authUrl)
                putExtra(EXTRA_IS_NONCE_AUTH, true)
                putExtra(EXTRA_USERNAME, tenantSlug)
                putExtra(EXTRA_QR_RESULT, authUrl)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            Toast.makeText(
                this,
                "Nonce authentication QR scanned successfully!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } else {
            showError("Could not extract tenant information from URL")
        }
    }

    /**
     * Extract tenant slug from auth URL
     * Example: https://negocio.alquilame.io/... -> "negocio"
     */
    private fun extractTenantSlugFromUrl(url: String): String? {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            // Extract subdomain (tenant slug)
            if (domain.contains(".alquilame.io")) {
                domain.substringBefore(".alquilame.io")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * LEGACY METHOD: Process old-style QR code with credentials
     * QR content format: "tenant_slug|tenant_secret_slug|hash"
     */
    private fun processLegacyQR(qrContent: String) {
        val parts = qrContent.split("|")

        if (parts.size == 3) {
            val tenantSlug = parts[0]
            val tenantSecretSlug = parts[1]
            val providedHash = parts[2]

            // Validate hash
            val expectedHash = generateHash(tenantSlug, tenantSecretSlug)

            if (providedHash == expectedHash) {
                // Hash is valid, return credentials
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_USERNAME, tenantSlug)
                    putExtra(EXTRA_API_KEY, tenantSecretSlug)
                    putExtra(EXTRA_IS_NONCE_AUTH, false)
                    putExtra(EXTRA_QR_RESULT, qrContent)
                }
                setResult(Activity.RESULT_OK, resultIntent)

                Toast.makeText(
                    this,
                    "Legacy QR code scanned successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                showError("Invalid QR code. Hash validation failed.")
            }
        } else {
            showError("Invalid QR code format. Please scan a valid Alquilame QR code.")
        }
    }

    private fun generateHash(tenantSlug: String, tenantSecretSlug: String): String {
        val input = "$tenantSlug|$tenantSecretSlug|${BuildConfig.SECRET_STRING_LOGIN}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}