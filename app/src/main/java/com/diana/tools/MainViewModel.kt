package com.diana.tools

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _rootDirUri = MutableLiveData<Uri>()
    private val _toolDirList = MutableLiveData<List<String>>()
    private val _idList = MutableLiveData<List<Int>>()

    val rootDirUri: MutableLiveData<Uri> = _rootDirUri
    val toolDirList: MutableLiveData<List<String>> = _toolDirList
    val idList: MutableLiveData<List<Int>> = _idList

    fun updateRootDirUri(rootDirUri: Uri) {
        _rootDirUri.value = rootDirUri
    }

    fun updateToolList(toolDirList: List<String>, idList: List<Int>) {
        _toolDirList.value = toolDirList
        _idList.value = idList
    }
}
