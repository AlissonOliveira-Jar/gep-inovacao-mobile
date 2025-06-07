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
            Log.d("DB_READ_FLOW", "Iniciando processo de leitura dos cupons...")
            try {
                val cupons = withContext(Dispatchers.IO) {
                    cupomDataSource.getAllSavedCupons()
                }
                Log.d("DB_READ_FLOW", "Leitura do banco de dados concluída. Número de cupons encontrados: ${cupons.size}")
                if (cupons.isNotEmpty()) {
                    Log.d("DB_READ_FLOW", "Primeiro cupom lido: ${cupons[0]}")
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

    fun deleteCupom(cupom: CupomInfo) {
        if (cupom.id == null) {
            Log.e("SavedCuponsVM", "Tentativa de deletar cupom com ID nulo.")
            return
        }
        viewModelScope.launch {
            val rowsDeleted = withContext(Dispatchers.IO) {
                cupomDataSource.deleteCupomById(cupom.id)
            }

            if (rowsDeleted > 0) {
                Log.d("SavedCuponsVM", "Cupom ID ${cupom.id} deletado. Recarregando a lista.")
                loadSavedCupons()
            } else {
                Log.w("SavedCuponsVM", "A deleção do cupom ID ${cupom.id} não afetou nenhuma linha.")
            }
        }
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }
}