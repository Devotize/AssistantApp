package com.sychev.assistantapp.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.sychev.assistantapp.R
import com.sychev.assistantapp.presentation.activity.main_activity.TAG

class WebViewComponent(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.web_veiw_layout, null)
    private val progressBar = rootView.findViewById<ProgressBar>(R.id.progress_bar_web_view)
    @SuppressLint("SetJavaScriptEnabled")
    private val webView = rootView.findViewById<WebView>(R.id.web_view).apply {
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadsImagesAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.javaScriptEnabled = true
        webChromeClient = object : WebChromeClient(){
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                }else {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
    private val previousButton = rootView.findViewById<ImageButton>(R.id.previous_button).apply {
        setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadUrl()
                setCurrentPageTextView()
            }
        }
    }
    private val nextButton = rootView.findViewById<ImageButton>(R.id.next_button).apply {
        setOnClickListener {
            if (currentPage < urls.size) {
                currentPage++
                loadUrl()
                setCurrentPageTextView()
            }
        }
    }
    private val closeButton = rootView.findViewById<ImageButton>(R.id.close_web_view_button).apply {
        setOnClickListener {
            hide()
        }
    }
    private val currentPageTextView = rootView.findViewById<TextView>(R.id.current_page_text_view)
    private val totalNumOfPagesTextView = rootView.findViewById<TextView>(R.id.total_page_text_view)
    private val windowParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }
    private val urls = ArrayList<String>()
    private var currentPage = 1

    fun setUrls(urls: List<String>) {
        this.urls.clear()
        this.urls.addAll(urls)
    }

    private fun loadUrl(){
        webView.loadUrl(urls[currentPage - 1])
    }

    private fun setCurrentPageTextView() {
        currentPageTextView.text = currentPage.toString()
    }

    private fun setTotalNumOfPages() {
        totalNumOfPagesTextView.text = urls.size.toString()
    }

    fun show() {
        if (rootView.parent == null) {
            loadUrl()
            setTotalNumOfPages()
            setCurrentPageTextView()
            windowManager.addView(rootView, windowParams)
        }
    }

    fun hide() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
        }
    }

}