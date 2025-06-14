package com.unichristus.leitor_fiscal.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.unichristus.leitor_fiscal.data.CupomDataSource
import com.unichristus.leitor_fiscal.data.CupomInfo
import com.unichristus.leitor_fiscal.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CupomDetailViewModel(application: Application, private val cupomId: Long) : AndroidViewModel(application) {

    private val cupomDataSource = CupomDataSource(application)

    private val _cupomDetails = MutableLiveData<CupomInfo?>()
    val cupomDetails: LiveData<CupomInfo?> = _cupomDetails

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _cupomDetails.value = withContext(Dispatchers.IO) { cupomDataSource.getCupomById(cupomId) }
            _products.value = withContext(Dispatchers.IO) { cupomDataSource.getProductsForCupom(cupomId) }
        }
    }
}

class CupomDetailViewModelFactory(private val application: Application, private val cupomId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CupomDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CupomDetailViewModel(application, cupomId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}