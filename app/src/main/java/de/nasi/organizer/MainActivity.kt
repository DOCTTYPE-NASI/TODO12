package de.nasi.organizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var loaded = false
    private var pendingOpen: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        web = WebView(this)
        web.setBackgroundColor(Color.BLACK)
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = true
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                loaded = true
                pendingOpen?.let { openTask(it) }
                pendingOpen = null
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "file") return false
                // Links nach außen im Browser öffnen
                try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")

        handleIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.__back?window.__back():false") { result ->
                    if (result != "true") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Änderungen aus dem Widget oder einer Benachrichtigung übernehmen
        if (loaded) web.evaluateJavascript("window.__reload&&window.__reload()", null)
    }

    private fun handleIntent(i: Intent?) {
        val id = i?.getStringExtra(EXTRA_TASK) ?: return
        i.removeExtra(EXTRA_TASK)
        if (loaded) openTask(id) else pendingOpen = id
    }

    private fun openTask(id: String) {
        web.evaluateJavascript(
            "window.__reload&&window.__reload();window.__openTask&&window.__openTask(" + JSONObject.quote(id) + ")",
            null
        )
    }

    /** Schnittstelle, die die Web-App als window.Android sieht */
    inner class Bridge {
        @JavascriptInterface
        fun load(): String = Store.load(applicationContext) ?: ""

        @JavascriptInterface
        fun save(json: String) {
            Store.save(applicationContext, json)
        }
    }

    companion object {
        const val EXTRA_TASK = "task_id"
    }
}
