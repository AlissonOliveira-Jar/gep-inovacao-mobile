package com.unichristus.leitor_fiscal.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.unichristus.leitor_fiscal.data.CupomInfo
import com.unichristus.leitor_fiscal.data.CupomDataSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedCuponsViewModel(application: Application) : AndroidViewModel(application) {

    private val cupomDataSource = CupomDataSource(application)

    private val _allSavedCupons = MutableLiveData<List<CupomInfo>>()
    val allSavedCupons: LiveData<List<CupomInfo>> get() = _allSavedCupons

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        loadSavedCupons()
    }

    fun loadSavedCupons() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val cupons = withContext(Dispatchers.IO) {
                    cupomDataSource.getAllSavedCupons()
                }
                _allSavedCupons.postValue(cupons)
            } catch (e: Exception) {
                Log.e("SavedCuponsVM", "Erro ao carregar cupons salvos", e)
                _errorMessage.postValue("Falha ao carregar cupons salvos.")
                _allSavedCupons.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }
}