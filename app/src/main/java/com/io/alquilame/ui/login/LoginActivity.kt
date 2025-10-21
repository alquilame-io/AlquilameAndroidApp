package com.io.alquilame.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.io.alquilame.databinding.ActivityLoginBinding
import com.io.alquilame.MainActivity

import com.io.alquilame.R

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    
    companion object {
        private const val QR_SCANNER_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if user is already logged in
        checkExistingLogin()

        val username = binding.username
        val apiKey = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory(this))
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                apiKey.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                setResult(Activity.RESULT_OK)
                updateUiWithUser(loginResult.success)
            }
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString().trim(),
                apiKey.text.toString().trim()
            )
        }

        apiKey.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString().trim(),
                    apiKey.text.toString().trim()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString().trim(),
                            apiKey.text.toString().trim()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(
                    username.text.toString().trim(),
                    apiKey.text.toString().trim()
                )
            }
        }
        
        // Setup registration link click listener
        binding.tvRegisterLink?.setOnClickListener {
            openRegistrationWebsite()
        }
        
        // Setup QR scanner button click listener
        binding.btnScanQr?.setOnClickListener {
            startQRScanner()
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_SHORT
        ).show()
        
        // Redirect to MainActivity after successful login
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
    
    private fun openRegistrationWebsite() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://alquilame.io"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open website. Please visit alquilame.io", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun startQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java)
        startActivityForResult(intent, QR_SCANNER_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == QR_SCANNER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                val isNonceAuth = intent.getBooleanExtra(QRScannerActivity.EXTRA_IS_NONCE_AUTH, false)

                if (isNonceAuth) {
                    // NEW METHOD: QR code contains nonce auth URL
                    handleNonceAuthQR(intent)
                } else {
                    // LEGACY METHOD: QR code contains credentials
                    handleLegacyQR(intent)
                }
            }
        }
    }

    /**
     * Handle new nonce-based QR code authentication
     */
    private fun handleNonceAuthQR(intent: Intent) {
        val authUrl = intent.getStringExtra(QRScannerActivity.EXTRA_AUTH_URL)?.trim()
        val tenantSlug = intent.getStringExtra(QRScannerActivity.EXTRA_USERNAME)?.trim()

        if (authUrl != null && tenantSlug != null) {
            // Save tenant slug temporarily for this session
            // Note: API key is not available from QR nonce, so we create a temporary session
            val credentialsStorage = com.io.alquilame.data.SecureCredentialsStorage(this)

            // Save tenant slug with a placeholder API key
            // User can set real API key later in settings for persistent login
            credentialsStorage.saveCredentials(tenantSlug, "QR_NONCE_SESSION")

            // Navigate directly to MainActivity with the auth URL
            // The WebView will open this URL directly without needing to generate a new nonce
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("AUTH_URL", authUrl)
                putExtra("TENANT_SLUG", tenantSlug)
                putExtra("IS_QR_NONCE_AUTH", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(mainIntent)
            finish()
        } else {
            Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle legacy QR code with credentials
     */
    private fun handleLegacyQR(intent: Intent) {
        val username = intent.getStringExtra(QRScannerActivity.EXTRA_USERNAME)?.trim()
        val apiKey = intent.getStringExtra(QRScannerActivity.EXTRA_API_KEY)?.trim()

        if (username != null && apiKey != null) {
            // Auto-fill the form fields
            binding.username.setText(username)
            binding.password.setText(apiKey)

            // Auto-login with QR credentials
            binding.loading.visibility = View.VISIBLE
            loginViewModel.login(username, apiKey)

            Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkExistingLogin() {
        val credentialsStorage = com.io.alquilame.data.SecureCredentialsStorage(this)
        if (credentialsStorage.hasCredentials()) {
            val apiKey = credentialsStorage.getApiKey()

            // Check if it's a valid API key or just a QR session placeholder
            if (apiKey == "QR_NONCE_SESSION") {
                // QR session expired, stay on login screen
                Toast.makeText(
                    this,
                    "Session expired. Please scan QR code or login with API key.",
                    Toast.LENGTH_LONG
                ).show()
                credentialsStorage.clearCredentials()
                return
            }

            // User is already logged in with valid API key, redirect to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}