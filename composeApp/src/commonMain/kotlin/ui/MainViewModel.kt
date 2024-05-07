package ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.domain.Repository
import data.domain.models.CurrencyRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MainViewModel : ScreenModel {

    private val repository = Repository()

    private val _currencyRatesUiState =
        MutableStateFlow<UiState<List<CurrencyRate>>>(UiState.Loading)

    val currencyRatesUiState: StateFlow<UiState<List<CurrencyRate>>> = _currencyRatesUiState
    private var currencyRateList: List<CurrencyRate> = emptyList()


    init {
        fetchCurrencyRates()
    }

    private fun fetchCurrencyRates() {
        screenModelScope.launch {
            repository.getCurrencyRates().flowOn(Dispatchers.IO).catch {
                _currencyRatesUiState.value = UiState.Error(it.message.toString())
            }.collect {
                _currencyRatesUiState.value = UiState.Success(it)
                currencyRateList = it
            }
        }
    }

    fun convertCurrency(amount: Double, currencyCode: String) {
        screenModelScope.launch(Dispatchers.Main) {
            repository.convertCurrencies(amount, currencyCode, currencyRateList)
                .onSuccess {
                    _currencyRatesUiState.value = UiState.Success(it)
                }
                .onFailure { e ->
                    _currencyRatesUiState.value = UiState.Error(e.toString())
                }
        }
    }
}

sealed interface UiState<out T> {
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    object Loading : UiState<Nothing>
}