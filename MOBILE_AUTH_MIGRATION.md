# Mobile App Authentication Migration Guide

## Overview

This document explains the new **nonce-based authentication** system for mobile webview authentication and how to migrate from the legacy dynamic secret method.

## Why Migrate?

The old `authenticate_with_dynamic_secret` method has several security issues:

1. **Hardcoded secrets** - The `SECRET_STRING_LOGIN` is hardcoded in the mobile app and can be extracted through reverse engineering
2. **Predictable tokens** - Uses timestamps which can be predicted within the 15-minute window
3. **Race conditions** - Potential timing issues between validation and marking as used
4. **Complex URL generation** - Requires generating SHA256 hashes on the mobile side

The new nonce-based authentication solves all these issues:

✅ **Cryptographically random** - Unpredictable 32-byte tokens
✅ **No hardcoded secrets** - All validation happens server-side
✅ **Atomic validation** - Database-level locking prevents race conditions
✅ **Simple integration** - Mobile app just requests a URL from the server
✅ **Better security** - Follows industry best practices (Curity nonce authenticator pattern)

## How It Works

### Two Authentication Methods

#### Method 1: API-Based (Programmatic)

```
Mobile App                    API Server                    WebView
    |                             |                             |
    |---(1) POST /api/mobile/auth/nonce------------------>|     |
    |    (with API key)            |                             |
    |                              |                             |
    |<--(2) {nonce, auth_url}------|                             |
    |                              |                             |
    |---(3) Open WebView with auth_url------------------------>|
    |                              |                             |
    |                              |<--(4) GET /auth/nonce/...---|
    |                              |                             |
    |                              |---(5) Validate & consume--->|
    |                              |     nonce (atomic)          |
    |                              |                             |
    |                              |---(6) Authenticate user---->|
    |                              |                             |
    |                              |---(7) Redirect to admin---->|
    |<--------------------------------(8) User sees admin panel--|
```

#### Method 2: QR Code-Based (User-Friendly)

```
Admin Panel                   Mobile App                    WebView
    |                             |                             |
    |---(1) User clicks           |                             |
    |     "Generate QR Code"      |                             |
    |                             |                             |
    |---(2) Server generates      |                             |
    |     nonce & QR code         |                             |
    |                             |                             |
    |---(3) QR code displayed---->|                             |
    |                             |                             |
    |                             |---(4) User scans QR code--->|
    |                             |                             |
    |                             |---(5) App extracts URL----->|
    |                             |                             |
    |                             |---(6) Opens WebView-------->|
    |                             |                             |
    |                             |     (Same as steps 4-8 above)
```

**Benefits of QR Code Method:**
- ✅ No need to type credentials
- ✅ One-click login from admin panel
- ✅ Perfect for initial setup or re-authentication
- ✅ Secure (nonce expires in 15 minutes, one-time use)
- ✅ Great user experience

## Migration Steps

### Step 1: Update Mobile App Code

#### Old Method (DEPRECATED)

```kotlin
// OLD - DO NOT USE
fun generateAuthUrl(tenantSlug: String, secretSlug: String): String {
    val timestamp = System.currentTimeMillis().toString()
    val secretString = "4KYKJkbv" // Hardcoded secret - INSECURE!

    val combined = "$tenantSlug|$secretSlug|$timestamp|$secretString"
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(combined.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .substring(0, 16)

    return "https://yourapp.com/s/tenant/$tenantSlug/$hash/$timestamp"
}
```

#### New Method (RECOMMENDED)

```kotlin
// NEW - SECURE
// Both values are found in: Settings → Integration → Mobile App Credentials
// - Tenant Slug: Identifies your account (e.g., "my-business")
// - API Key: Secure authentication token (e.g., "abc123xyz...")

suspend fun getAuthUrl(apiKey: String, tenantSlug: String): Result<String> {
    val client = OkHttpClient()

    val deviceInfo = JSONObject().apply {
        put("os", "Android")
        put("version", Build.VERSION.RELEASE)
        put("model", Build.MODEL)
        put("app_version", BuildConfig.VERSION_NAME)
    }

    val requestBody = JSONObject().apply {
        put("device_info", deviceInfo)
    }.toString()

    val request = Request.Builder()
        .url("https://yourapp.com/api/mobile/auth/nonce")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(requestBody.toRequestBody("application/json".toMediaType()))
        .build()

    return try {
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "")

        if (json.getBoolean("success")) {
            val data = json.getJSONObject("data")
            Result.success(data.getString("auth_url"))
        } else {
            Result.failure(Exception(json.getString("message")))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Usage in your activity/fragment
lifecycleScope.launch {
    when (val result = getAuthUrl(apiKey, tenantSlug)) {
        is Result.Success -> {
            // Open webview with the secure auth URL
            webView.loadUrl(result.value)
        }
        is Result.Failure -> {
            // Handle error
            Log.e("Auth", "Failed to get auth URL", result.exception)
        }
    }
}
```

### Step 2: Add QR Code Scanner (Optional but Recommended)

For the best user experience, add QR code scanning capability:

```kotlin
// Add dependency to build.gradle
dependencies {
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
}

// QR Code scanner activity/fragment
class QRCodeScannerActivity : AppCompatActivity() {
    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents != null) {
            handleScannedUrl(result.contents)
        }
    }

    fun startScanning() {
        barcodeLauncher.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan QR code from admin panel")
            setBeepEnabled(true)
            setOrientationLocked(false)
        })
    }

    private fun handleScannedUrl(url: String) {
        // Validate it's a nonce auth URL
        if (url.contains("/auth/nonce/")) {
            // Open WebView with the scanned URL
            openAuthWebView(url)
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAuthWebView(url: String) {
        val intent = Intent(this, AuthWebViewActivity::class.java).apply {
            putExtra("AUTH_URL", url)
        }
        startActivity(intent)
    }
}

// In your main activity, add a button for QR scanning
binding.btnScanQR.setOnClickListener {
    startActivity(Intent(this, QRCodeScannerActivity::class.java))
}
```

**QR Code Flow:**
1. User clicks "Scan QR Code" in mobile app
2. Camera opens
3. User scans QR code from admin panel (Settings → Integration → Generate Login QR Code)
4. App extracts authentication URL from QR code
5. WebView opens with nonce URL
6. User is instantly authenticated!
```

### Step 2: Update API Endpoint

The new endpoint is:

```
POST https://yourapp.com/api/mobile/auth/nonce
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json

{
  "device_info": {
    "os": "Android",
    "version": "13",
    "model": "Pixel 6",
    "app_version": "1.2.3"
  }
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "nonce": "Kx7pQmR9... (32-byte random string)",
    "auth_url": "https://yourapp.com/auth/nonce/tenant-slug/Kx7pQmR9...",
    "expires_at": "2025-10-21T11:00:00Z"
  },
  "message": "Authentication nonce generated successfully"
}
```

### Step 3: Test Authentication

1. Call the nonce generation endpoint
2. Receive the `auth_url`
3. Open a WebView with the `auth_url`
4. User should be automatically logged in and redirected to admin panel
5. The nonce can only be used **once** - subsequent attempts will fail

## Security Features

### 1. One-Time Use

Each nonce can only be used once. If a user tries to use the same URL twice:

```json
{
  "error": "This authentication link has already been used"
}
```

### 2. Time Expiration

Nonces expire after 15 minutes:

```json
{
  "error": "This authentication link has expired"
}
```

### 3. Tenant Isolation

Nonces are tied to specific tenants. A nonce for tenant A cannot authenticate into tenant B.

### 4. Audit Trail

The system logs:
- Device information
- IP address
- Generation timestamp
- Usage timestamp

## Backward Compatibility

The old authentication methods are still supported for backward compatibility:

- `/s/tenant/:tenant_slug/:secret_tenant_slug_dynamic/:random_key` (DEPRECATED)
- `/s/:secret_tenant_slug` (DEPRECATED)

**Migration Timeline:**

1. **Phase 1 (Current)**: Both methods work
2. **Phase 2 (3 months)**: Legacy methods show deprecation warnings
3. **Phase 3 (6 months)**: Legacy methods are removed

## Testing

### Getting Your Credentials

To configure your mobile app, you need **two values** from the admin panel:

1. Log in to your admin panel
2. Go to **Settings** (gear icon in navigation)
3. Click on **Integration** in the sidebar
4. Find the **Mobile App Credentials** section
5. If you don't have an API key yet, click **Generate API Key**
6. Copy both values:
   - **Tenant Slug**: Your unique account identifier (e.g., `my-business`)
   - **API Key**: Your secure authentication token (keep it secret!)

Both values are required for the mobile app to function.

### Manual Testing with cURL

```bash
# 1. Get your API key (from tenant settings > integration)
API_KEY="your-api-key-here"

# 2. Request a nonce
curl -X POST https://yourapp.com/api/mobile/auth/nonce \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "device_info": {
      "os": "Android",
      "version": "13",
      "model": "Test Device"
    }
  }'

# 3. Copy the auth_url from response and open in browser
# You should be automatically logged in!
```

### Integration Tests

Add tests to verify:

1. Nonce generation returns valid URL
2. Opening the URL authenticates the user
3. Using the same URL twice fails
4. Expired nonces are rejected

## Troubleshooting

### Error: "Invalid authentication link"

**Cause**: Nonce doesn't exist or tenant mismatch

**Solution**: Generate a fresh nonce

### Error: "This authentication link has already been used"

**Cause**: Attempting to reuse a nonce

**Solution**: Generate a new nonce for each authentication attempt

### Error: "This authentication link has expired"

**Cause**: More than 15 minutes passed since nonce generation

**Solution**: Generate a fresh nonce

### Error: "Invalid or missing API key"

**Cause**: API key not provided or invalid

**Solution**: Check that you're passing the correct API key in the Authorization header

## Database Cleanup

Expired and used nonces are automatically cleaned up every hour to prevent database bloat. You can also manually trigger cleanup:

```elixir
# In IEx console
Rent.AuthNonce.cleanup_old_nonces()
```

## Monitoring

Check nonce statistics for a tenant:

```elixir
# In IEx console
Rent.AuthNonce.get_nonce_stats(tenant_id)

# Returns:
# %{
#   total: 1500,      # Total nonces generated
#   active: 5,        # Valid unused nonces
#   used: 1450,       # Used nonces
#   expired: 45       # Expired unused nonces
# }
```

## Support

If you encounter issues during migration:

1. Check the application logs for detailed error messages
2. Verify your API key is valid and active
3. Ensure your app has network connectivity
4. Contact the backend team with:
   - Tenant slug
   - Timestamp of the issue
   - Full error message

## References

- [Nonce Authenticator Pattern](https://curity.io/resources/learn/nonce-authenticator-pattern/)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [OAuth 2.0 for Mobile Apps](https://www.rfc-editor.org/rfc/rfc8252)
