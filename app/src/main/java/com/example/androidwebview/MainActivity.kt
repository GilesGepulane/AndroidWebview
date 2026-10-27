package com.example.androidwebview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val appHost = "10.0.2.2"
    private val appUrl = "http://$appHost:5000"
    private val localNet = "android.permission.ACCESS_LOCAL_NETWORK"

    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) webView.loadUrl(appUrl)
            else showError("Local network permission was denied.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        ViewCompat.setOnApplyWindowInsetsListener(webView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()   // enables confirm()/alert()
        webView.webViewClient = object : WebViewClient() {

            // Keep our pages inside the WebView; send everything else out
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                if (uri.host == appHost) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: ActivityNotFoundException) {
                    Log.w("WebView", "No app to open $uri")
                }
                return true
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.e("WebView", "${error.description} for ${request.url}")
                    showError("Can't reach the server (${error.description}).")
                }
            }

            override fun onReceivedHttpError(
                view: WebView, request: WebResourceRequest, response: WebResourceResponse
            ) {
                if (request.isForMainFrame) showError("Server error ${response.statusCode}.")
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })

        if (ContextCompat.checkSelfPermission(this, localNet) == PackageManager.PERMISSION_GRANTED) {
            webView.loadUrl(appUrl)
        } else {
            requestLocalNetwork.launch(localNet)
        }
    }

    private fun showError(message: String) {
        val html = """
            <html><body style="font-family:sans-serif;text-align:center;padding:48px 24px;
                background:#CBD5E1;color:#2B4C7E">
            <h2>Something went wrong</h2><p>$message</p>
            <p><a href="$appUrl" style="display:inline-block;padding:14px 28px;border-radius:99px;
                background:#FFA630;color:#2B4C7E;text-decoration:none;font-weight:bold">Retry</a></p>
            </body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(appUrl, html, "text/html", "utf-8", null)
    }
}