package com.diana.tools.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.diana.tools.MainViewModel
import com.diana.tools.databinding.FragmentWebpageViewerBinding

class WebpageViewerFragment : Fragment() {

    private var _binding: FragmentWebpageViewerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var rootUri: String
    private lateinit var dirName: String
    private val mainViewModel: MainViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootUri = requireArguments().getString("root_uri")!!
        dirName = requireArguments().getString("dir_name")!!

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebpageViewerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val webpageViewer: WebView = binding.webpageViewer
        webpageViewer.settings.apply {
            javaScriptEnabled = true
        }
        webpageViewer.isHorizontalScrollBarEnabled = false
        webpageViewer.isVerticalScrollBarEnabled = false
        webpageViewer.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()
                val context = context ?: return null

//                correct "content://" uri
                if (url.startsWith("content://")) {
                    return try {
                        val relativePath = url.substring((rootUri + "/").length)
                        val correctFileUri = DocumentsContract.buildDocumentUriUsingTree(
                            rootUri.toUri(),
                            DocumentsContract.getTreeDocumentId(rootUri.toUri()) + "/${dirName}/${relativePath}"
                        )
                        getWebResourceResponse(correctFileUri, context)
                    } catch (_: Exception) {
                        null
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webpageViewer.loadUrl(rootUri + "/index.html")
        mainViewModel.refreshWebViewEvent.observe(viewLifecycleOwner, Observer {
            webpageViewer.reload()
        })
        return root
    }

    private fun getWebResourceResponse(uri: Uri, context: Context): WebResourceResponse? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.let {
                val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
                WebResourceResponse(mimeType, "UTF-8", it)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
