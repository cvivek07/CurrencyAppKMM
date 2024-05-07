package data.domain

import data.domain.models.CurrencyRate
import data.domain.models.LatestRatesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.roundToLong

class Repository {
    private val httpClient = HttpClient() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getCurrencyRates(): Flow<List<CurrencyRate>> =
        flow {
            val currencyRates = mutableListOf<CurrencyRate>()
            val latestRatesResponse: LatestRatesResponse =
                httpClient.get("https://openexchangerates.org/api/latest.json") {
                    url {
                        parameters.append("app_id", "461995c5eb404a59a02d880b27ca0b7c")
                    }
                }.body()

            val currenciesResponse: Map<String, String> =
                httpClient.get("https://openexchangerates.org/api/currencies.json") {
                    url {
                        parameters.append("app_id", "461995c5eb404a59a02d880b27ca0b7c")
                    }
                }.body()

            val baseRates = latestRatesResponse.rates
            for (currency in currenciesResponse) {
                currencyRates.add(
                    CurrencyRate(
                        currency.key,
                        currency.value,
                        if (baseRates[currency.key] != null) baseRates[currency.key]!!.round() else 1.0
                    )
                )
            }
            emit(currencyRates)
        }

    suspend fun convertCurrencies(
        amount: Double,
        selectedCurrency: String,
        currencyRates: List<CurrencyRate>
    ): Result<List<CurrencyRate>> {
        return if (currencyRates.isEmpty()) {
            Result.failure(IllegalArgumentException("Empty currency rates list"))
        } else {
            val baseRateFromCurrency: Double =
                getBaseRateForCurrency(currencyRates, selectedCurrency)
            computationCall {
                currencyRates.map { c ->
                    CurrencyRate(
                        c.currencyCode,
                        c.currencyName,
                        convertCurrency(amount, baseRateFromCurrency, c.baseRate).round()
                    )
                }
            }
        }
    }

    private fun convertCurrency(
        amount: Double,
        baseRateFromCurrency: Double,
        baseRateToCurrency: Double
    ): Double {
        return amount * baseRateToCurrency / baseRateFromCurrency
    }

    private fun getBaseRateForCurrency(
        exchangeRates: List<CurrencyRate>,
        selectedCurrencyCode: String
    ): Double {
        return exchangeRates.find { c -> c.currencyCode == selectedCurrencyCode }?.baseRate
            ?: 1.0
    }

    private suspend fun <T> computationCall(
        call: suspend () -> T
    ): Result<T> = runCatching {
        withContext(Dispatchers.Default) {
            call.invoke()
        }
    }

    fun Double.round(): Double {
        return (this * 100).roundToLong() / 100.0
    }
}