package com.luckytide.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Lucky Tide — app Android bọc cổng game (lobby + game) trong WebView toàn màn hình.
 * Lần đầu mở: nhập địa chỉ server (link VPS / link trycloudflare / http://IP:3000). Đổi lại bất cứ lúc nào:
 * bấm Back ở lobby -> "Change server", hoặc tự hiện khi không kết nối được.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private var web: WebView? = null
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private val prefs by lazy { getSharedPreferences("lucky_tide", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)   // không tắt màn khi đang chơi
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        hideSystemBars()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val w = web
                when {
                    customView != null -> exitFullscreenView()
                    w != null && w.canGoBack() -> w.goBack()
                    else -> AlertDialog.Builder(this@MainActivity)
                        .setMessage("Exit Lucky Tide?")
                        .setPositiveButton("Exit") { _, _ -> finish() }
                        .setNeutralButton("Change server") { _, _ -> showSetup(null) }
                        .setNegativeButton("Cancel", null).show()
                }
            }
        })

        val url = prefs.getString("server_url", null)
        if (url.isNullOrBlank()) showSetup(null) else openPortal(url)
    }

    // ---------------- màn hình nhập địa chỉ server ----------------
    private fun showSetup(error: String?) {
        destroyWeb()
        root.removeAllViews()
        val pad = dp(24)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(0xFF21093F.toInt()); setStroke(dp(2), 0xFF6F58FF.toInt()) }
        }
        val title = TextView(this).apply {
            text = "LUCKY TIDE"; textSize = 30f; setTextColor(0xFFFFD400.toInt()); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        }
        val hintTv = TextView(this).apply {
            text = "Server address (ask your cashier)"; textSize = 14f; setTextColor(0xFFCBBCFF.toInt()); setPadding(0, dp(12), 0, dp(6))
        }
        val input = EditText(this).apply {
            setText(prefs.getString("server_url", null) ?: BuildConfig.SERVER_URL)
            setSingleLine(); inputType = InputType.TYPE_TEXT_VARIATION_URI; imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(Color.WHITE); setHintTextColor(0x88FFFFFF.toInt()); hint = "https://game.example.com"
            background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(0xFF12042A.toInt()); setStroke(dp(1), 0xFF5A3AA0.toInt()) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val err = TextView(this).apply { text = error ?: ""; setTextColor(0xFFFF6A6A.toInt()); textSize = 13f; setPadding(0, dp(8), 0, 0) }
        val btn = Button(this).apply {
            text = "CONNECT"; setTextColor(Color.WHITE); textSize = 17f; typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(0xFFF07000.toInt()) }
        }
        val go = {
            var u = input.text.toString().trim().trimEnd('/')
            if (u.isNotEmpty() && !u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
            if (Uri.parse(u).host.isNullOrBlank()) { err.text = "Please enter a valid address" }
            else { prefs.edit().putString("server_url", u).apply(); openPortal(u) }
        }
        btn.setOnClickListener { go() }
        input.setOnEditorActionListener { _, _, _ -> go(); true }
        box.addView(title); box.addView(hintTv)
        box.addView(input, LinearLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT))
        box.addView(err)
        box.addView(btn, LinearLayout.LayoutParams(dp(420), dp(50)).apply { topMargin = dp(14) })
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val center = FrameLayout(this)
        center.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        scroll.addView(center)
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    // ---------------- WebView chạy lobby + game ----------------
    @SuppressLint("SetJavaScriptEnabled")
    private fun openPortal(url: String) {
        root.removeAllViews()
        destroyWeb()
        val home = Uri.parse(url)
        val w = WebView(this)
        web = w
        w.setBackgroundColor(Color.BLACK)
        w.setLayerType(View.LAYER_TYPE_HARDWARE, null)                                   // vẽ WebGL bằng GPU
        if (Build.VERSION.SDK_INT >= 26) w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)   // ưu tiên tiến trình vẽ game
        w.isVerticalScrollBarEnabled = false; w.isHorizontalScrollBarEnabled = false
        w.overScrollMode = View.OVER_SCROLL_NEVER
        with(w.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true                       // lưu đăng nhập (localStorage)
            mediaPlaybackRequiresUserGesture = false       // âm thanh game
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT                // giữ cache -> lần sau vào game nhanh
            offscreenPreRaster = true
            setGeolocationEnabled(false)
            userAgentString = "$userAgentString LuckyTideApp/1.0"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val u = req.url
                if (u.host == home.host) return false                        // trang của server: mở trong app
                runCatching { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, u)) }  // link ngoài (LINKS): mở trình duyệt
                return true
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, error: WebResourceError) {
                if (req.isForMainFrame) showSetup("Cannot connect to $url\n(${error.description}). Check the address or your internet.")
            }
        }
        w.webChromeClient = object : WebChromeClient() {
            // nút toàn màn hình trong khung game
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view; customCallback = callback
                root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                hideSystemBars()
            }
            override fun onHideCustomView() = exitFullscreenView()
        }
        root.addView(w, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        w.loadUrl(url)
    }

    private fun exitFullscreenView() {
        customView?.let { root.removeView(it) }
        customView = null
        customCallback?.onCustomViewHidden(); customCallback = null
        hideSystemBars()
    }

    private fun destroyWeb() {
        web?.let { root.removeView(it); it.stopLoading(); it.destroy() }
        web = null
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemBars() }
    override fun onPause() { super.onPause(); web?.onPause() }
    override fun onResume() { super.onResume(); web?.onResume() }
    override fun onDestroy() { destroyWeb(); super.onDestroy() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
