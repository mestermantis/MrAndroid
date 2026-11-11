package ir.hafaonline.manager

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var themeButton: Button
    private var darkMode = true
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val panelUrl = "https://hafaonline.ir/manager"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        configureWebView()

        if (savedInstanceState == null) {
            web.loadUrl(panelUrl)
        } else {
            web.restoreState(savedInstanceState)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 20, 35))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(Color.rgb(21, 31, 50))
        }

        val brand = TextView(this).apply {
            text = "HAFA  مدیریت"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        toolbar.addView(brand, LinearLayout.LayoutParams(0, dp(48), 1f))

        val reloadButton = Button(this).apply {
            text = "تازه‌سازی"
            textSize = 11f
            setOnClickListener { web.reload() }
        }
        toolbar.addView(reloadButton)

        themeButton = Button(this).apply {
            text = "روشن"
            textSize = 11f
            setOnClickListener { toggleTheme() }
        }
        toolbar.addView(themeButton)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }

        web = WebView(this)
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(54)))
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        updateSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (Build.VERSION.SDK_INT >= 29) {
                forceDark = if (darkMode) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            }
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme == "https" && uri.host == "hafaonline.ir") return false
                if (uri.scheme == "http" || uri.scheme == "https") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progress.visibility = View.GONE
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    val chooser = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    startActivityForResult(chooser, REQUEST_FILE)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    Toast.makeText(this@MainActivity, "انتخاب فایل ممکن نیست", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        web.setDownloadListener { url, _, _, mimeType, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (!mimeType.isNullOrBlank()) intent.type = mimeType
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "باز کردن فایل ممکن نیست", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleTheme() {
        darkMode = !darkMode
        if (Build.VERSION.SDK_INT >= 29) {
            web.settings.forceDark = if (darkMode) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
        themeButton.text = if (darkMode) "روشن" else "تیره"
        updateSystemBars()
        web.reload()
    }

    private fun updateSystemBars() {
        val color = if (darkMode) Color.rgb(12, 20, 35) else Color.rgb(245, 247, 250)
        window.statusBarColor = color
        window.navigationBarColor = color
        if (Build.VERSION.SDK_INT >= 26) {
            window.decorView.systemUiVisibility =
                if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    @Deprecated("Deprecated by Android; retained for WebView file chooser compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE) {
            val result = if (resultCode == RESULT_OK) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            fileCallback?.onReceiveValue(result)
            fileCallback = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        web.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) {
            web.goBack()
        } else {
            AlertDialog.Builder(this)
                .setMessage("از برنامه مدیریت خارج شوید؟")
                .setNegativeButton("ادامه کار", null)
                .setPositiveButton("خروج") { _, _ -> finish() }
                .show()
        }
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_FILE = 4102
        private const val REQUEST_NOTIFICATIONS = 4103
    }
}
