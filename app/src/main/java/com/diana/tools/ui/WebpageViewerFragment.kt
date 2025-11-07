package com.diana.tools.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.diana.tools.databinding.FragmentWebpageViewerBinding

class WebpageViewerFragment : Fragment() {

    private var _binding: FragmentWebpageViewerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var rootUri: String
    private lateinit var dirName: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootUri = requireArguments().getString("root_uri")!!
        dirName = requireArguments().getString("dirname")!!

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebpageViewerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val webpageViewer: WebView = binding.webpageViewer
        webpageViewer.settings.javaScriptEnabled = true
        webpageViewer.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()
                val context = context ?: return null

                // 1. 只处理 content:// 协议的请求
                if (url.startsWith("content://")) {
                    return try {
                        val relativePath = url.substring((rootUri + "/").length)
                        val correctFileUri = DocumentsContract.buildDocumentUriUsingTree(
                            Uri.parse(rootUri), // 注意：这里要用你原始的、正确的目录树 Uri String
                            DocumentsContract.getTreeDocumentId(Uri.parse(rootUri)) + "/${dirName}/${relativePath}"
                        )
                        // 4. 使用修复后的正确 Uri 来获取资源
                        getWebResourceResponse(correctFileUri, context)

                    } catch (e: Exception) {
                        Log.e("WebView", "加载资源失败: $url, 错误: ${e.message}")
                        null
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }


        val webUri = DocumentsContract.buildDocumentUriUsingTree(
            rootUri.toUri(),
            DocumentsContract.getTreeDocumentId(rootUri.toUri()) + "/$dirName/index.html"
        )

        val htmlContent = try {
            context?.contentResolver?.openInputStream(webUri)?.bufferedReader()?.readText()
        } catch (e: Exception) {
            Log.e("WebView", "读取 HTML 失败: ${e.message}")
            null
        }

        if (htmlContent != null) {
            // 使用 SAF 目录的 Uri 作为 baseUrl，确保相对路径能被正确解析
            val baseUrl = rootUri + "/"
            webpageViewer.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 辅助函数，用于统一获取 WebResourceResponse
    private fun getWebResourceResponse(uri: Uri, context: Context): WebResourceResponse? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("WebView", "无法打开资源输入流: $uri")
                return null
            }
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
            WebResourceResponse(mimeType, "UTF-8", inputStream)
        } catch (e: Exception) {
            Log.e("WebView", "获取资源响应失败: $uri, 错误: ${e.message}")
            null
        }
    }
}
