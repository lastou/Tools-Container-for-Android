package com.diana.tools.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val _toolDirList = MutableLiveData<List<String>>()
    private val _idList = MutableLiveData<List<Int>>()

    val toolDirList: MutableLiveData<List<String>> = _toolDirList
    val idList: MutableLiveData<List<Int>> = _idList

    fun setData(toolDirList: List<String>, idList: List<Int>) {
        _toolDirList.value = toolDirList
        _idList.value = idList
    }
}
