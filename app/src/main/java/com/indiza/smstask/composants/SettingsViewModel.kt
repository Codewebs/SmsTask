package com.indiza.smstask.composants

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.smstask.ApiClient
import com.indiza.smstask.tools.DataStoreManager
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val ds = DataStoreManager(app)

    val baseUrl = ds.baseUrl

    fun saveUrl(url: String) {
        viewModelScope.launch {
            ds.setBaseUrl(url)
            ApiClient.init(url)   // 🔥 IMPORTANT
        }
    }

}
