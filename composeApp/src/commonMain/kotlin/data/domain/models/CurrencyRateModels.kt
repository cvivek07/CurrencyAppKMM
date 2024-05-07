package data.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias CurrencyMap = Map<String, Double>

@Serializable
data class LatestRatesResponse(
    @SerialName("disclaimer") val disclaimer: String,
    @SerialName("license") val license: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("base") val base: String,
    @SerialName("rates") val rates: CurrencyMap
)

data class CurrencyRate(
    val currencyCode: String,
    val currencyName: String,
    val baseRate: Double
) {
    override fun toString(): String {
        return "$currencyCode - $currencyName"
    }
}