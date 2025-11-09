package com.diana.tools

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _rootDirUri = MutableLiveData<Uri>()
    private val _toolDirList = MutableLiveData<List<String>>()
    private val _idList = MutableLiveData<List<Int>>()
    private val _refreshWebViewEvent = MutableLiveData<Unit>()

    val rootDirUri: MutableLiveData<Uri> = _rootDirUri
    val toolDirList: MutableLiveData<List<String>> = _toolDirList
    val idList: MutableLiveData<List<Int>> = _idList
    val refreshWebViewEvent: MutableLiveData<Unit> = _refreshWebViewEvent

    fun updateRootDirUri(rootDirUri: Uri) {
        _rootDirUri.value = rootDirUri
    }

    fun updateToolList(toolDirList: List<String>, idList: List<Int>) {
        _toolDirList.value = toolDirList
        _idList.value = idList
    }

    // 提供一个公共方法来触发刷新事件
    fun refreshWebView() {
        _refreshWebViewEvent.value = Unit
    }
}
