package com.example.util

import com.example.data.model.PsaPricingConfig
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

data class PsaCountryConfig(
    val code: String,
    val name: String,
    val flag: String,
    val localeLanguage: String,
    val localeCountry: String,
    val currencyCode: String,
    val currencySymbol: String,
    val officialSiteUrl: String,
    val officialStoreUrl: String,
    val promotionsUrl: String,
    val dipLoginUrl: String = OFFICIAL_DIP_LOGIN_URL,
    val hasPublicStore: Boolean = false,
    val usesDecimalSeparatorComma: Boolean = true,
    val decimalDigits: Int = 0
) {
    fun toLocale(): Locale = Locale(localeLanguage, localeCountry)

    val flagEmoji: String
        get() = flag

    val shortBadge: String
        get() = "$flag $code • $currencyCode"

    companion object {
        const val OFFICIAL_DIP_LOGIN_URL = "https://mi.psa.com.ar/login"
    }
}

object PsaCountryRegistry {

    val countries: List<PsaCountryConfig> = listOf(
        PsaCountryConfig(
            code = "AR",
            name = "Argentina",
            flag = "🇦🇷",
            localeLanguage = "es",
            localeCountry = "AR",
            currencyCode = "ARS",
            currencySymbol = "$",
            officialSiteUrl = "https://www.psa.com.ar/",
            officialStoreUrl = "https://tienda.psa.com.ar/",
            promotionsUrl = "https://tienda.psa.com.ar/promociones_vigentes",
            hasPublicStore = true
        ),
        PsaCountryConfig(
            code = "UY",
            name = "Uruguay",
            flag = "🇺🇾",
            localeLanguage = "es",
            localeCountry = "UY",
            currencyCode = "UYU",
            currencySymbol = "\$U",
            officialSiteUrl = "https://uruguay.psa.com.ar/",
            officialStoreUrl = "https://tienda.psa.com.uy/",
            promotionsUrl = "https://tienda.psa.com.uy/promociones_vigentes-uruguay",
            hasPublicStore = true
        ),
        PsaCountryConfig(
            code = "CL",
            name = "Chile",
            flag = "🇨🇱",
            localeLanguage = "es",
            localeCountry = "CL",
            currencyCode = "CLP",
            currencySymbol = "$",
            officialSiteUrl = "https://chile.psa.com.ar/",
            officialStoreUrl = "https://chile.psa.com.ar/",
            promotionsUrl = "https://chile.psa.com.ar/",
            hasPublicStore = false
        ),
        PsaCountryConfig(
            code = "BO",
            name = "Bolivia",
            flag = "🇧🇴",
            localeLanguage = "es",
            localeCountry = "BO",
            currencyCode = "BOB",
            currencySymbol = "Bs",
            officialSiteUrl = "https://bolivia.psa.com.ar/",
            officialStoreUrl = "https://bolivia.psa.com.ar/",
            promotionsUrl = "https://bolivia.psa.com.ar/",
            hasPublicStore = false
        ),
        PsaCountryConfig(
            code = "PY",
            name = "Paraguay",
            flag = "🇵🇾",
            localeLanguage = "es",
            localeCountry = "PY",
            currencyCode = "PYG",
            currencySymbol = "₲",
            officialSiteUrl = "https://paraguay.psa.com.ar/",
            officialStoreUrl = "https://paraguay.psa.com.ar/",
            promotionsUrl = "https://paraguay.psa.com.ar/",
            hasPublicStore = false
        ),
        PsaCountryConfig(
            code = "PE",
            name = "Perú",
            flag = "🇵🇪",
            localeLanguage = "es",
            localeCountry = "PE",
            currencyCode = "PEN",
            currencySymbol = "S/",
            officialSiteUrl = "https://peru.psa.com.ar/",
            officialStoreUrl = "https://peru.psa.com.ar/",
            promotionsUrl = "https://peru.psa.com.ar/",
            hasPublicStore = false
        ),
        PsaCountryConfig(
            code = "CO",
            name = "Colombia",
            flag = "🇨🇴",
            localeLanguage = "es",
            localeCountry = "CO",
            currencyCode = "COP",
            currencySymbol = "$",
            officialSiteUrl = "https://colombia.psa.com.ar/",
            officialStoreUrl = "https://colombia.psa.com.ar/",
            promotionsUrl = "https://colombia.psa.com.ar/",
            hasPublicStore = false
        ),
        PsaCountryConfig(
            code = "EC",
            name = "Ecuador",
            flag = "🇪🇨",
            localeLanguage = "es",
            localeCountry = "EC",
            currencyCode = "USD",
            currencySymbol = "US$",
            officialSiteUrl = "https://ecuador.psa.com.ar/",
            officialStoreUrl = "https://ecuador.psa.com.ar/",
            promotionsUrl = "https://ecuador.psa.com.ar/",
            hasPublicStore = false,
            usesDecimalSeparatorComma = false,
            decimalDigits = 0
        ),
        PsaCountryConfig(
            code = "ES",
            name = "España",
            flag = "🇪🇸",
            localeLanguage = "es",
            localeCountry = "ES",
            currencyCode = "EUR",
            currencySymbol = "€",
            officialSiteUrl = "https://www.psa.com.es/",
            officialStoreUrl = "https://www.psa.com.es/",
            promotionsUrl = "https://www.psa.com.es/",
            hasPublicStore = false
        )
    )

    fun defaultCountry(): PsaCountryConfig = countries.first()

    fun getByCode(code: String?): PsaCountryConfig {
        val clean = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return countries.firstOrNull { it.code == clean } ?: countries.first()
    }

    fun currencyNumberFormat(country: PsaCountryConfig): NumberFormat {
        val symbols = DecimalFormatSymbols(country.toLocale()).apply {
            currencySymbol = country.currencySymbol
            if (country.usesDecimalSeparatorComma) {
                groupingSeparator = '.'
                decimalSeparator = ','
            } else {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
        }
        val pattern = if (country.decimalDigits > 0) {
            "¤#,##0.${"0".repeat(country.decimalDigits)}"
        } else {
            "¤#,##0"
        }
        return DecimalFormat(pattern, symbols).apply {
            maximumFractionDigits = country.decimalDigits
            minimumFractionDigits = country.decimalDigits
        }
    }

    fun formatCurrency(amount: Double, country: PsaCountryConfig, includeCode: Boolean = false): String {
        val symbols = DecimalFormatSymbols(country.toLocale()).apply {
            if (country.usesDecimalSeparatorComma) {
                groupingSeparator = '.'
                decimalSeparator = ','
            } else {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
        }
        val pattern = if (country.decimalDigits > 0) "#,##0.${"0".repeat(country.decimalDigits)}" else "#,##0"
        val formatter = DecimalFormat(pattern, symbols)
        val formattedNum = formatter.format(amount)
        return if (includeCode) {
            "${country.currencySymbol}$formattedNum ${country.currencyCode}"
        } else {
            "${country.currencySymbol}$formattedNum"
        }
    }

    /**
     * Baseline country pricing prior to live official sync (AR reflects verified real tienda.psa.com.ar PVP).
     */
    fun defaultPricingForCountry(countryCode: String): PsaPricingConfig {
        return when (countryCode.uppercase(Locale.ROOT)) {
            "AR" -> PsaPricingConfig(
                bidonPrice = 4800.0,
                bottlePrice = 1800.0,
                senior4Price = 950000.0,
                quantumPrice = 1298000.0,
                senikPrice = 1298000.0,
                cromaPrice = 781000.0,
                veroPrice = 549000.0,
                s1000Price = 1100000.0,
                miniPrice = 427000.0,
                c3Price = 248000.0,
                duchaPrice = 216000.0,
                descalcificadorPrice = 240000.0,
                domusPrice = 1650000.0,
                maximaPrice = 1150000.0,
                piletaPrice = 890000.0,
                airePrice = 690000.0,
                ropotPrice = 1490000.0,
                rotechPrice = 2150000.0,
                botellaTermicaPrice = 92000.0,
                repuestoSeniorPrice = 103000.0,
                installmentsCount = 12
            )
            "UY" -> PsaPricingConfig(
                bidonPrice = 190.0,
                bottlePrice = 70.0,
                senior4Price = 32900.0,
                quantumPrice = 44900.0,
                senikPrice = 44900.0,
                cromaPrice = 26900.0,
                veroPrice = 19900.0,
                s1000Price = 37900.0,
                miniPrice = 14900.0,
                c3Price = 8600.0,
                duchaPrice = 7800.0,
                descalcificadorPrice = 8900.0,
                domusPrice = 58000.0,
                maximaPrice = 39900.0,
                piletaPrice = 31000.0,
                airePrice = 24500.0,
                ropotPrice = 52000.0,
                rotechPrice = 75000.0,
                botellaTermicaPrice = 3200.0,
                repuestoSeniorPrice = 3600.0,
                installmentsCount = 12
            )
            "CL" -> PsaPricingConfig(
                bidonPrice = 3500.0,
                bottlePrice = 1200.0,
                senior4Price = 690000.0,
                quantumPrice = 920000.0,
                senikPrice = 920000.0,
                cromaPrice = 560000.0,
                veroPrice = 410000.0,
                s1000Price = 780000.0,
                miniPrice = 320000.0,
                c3Price = 185000.0,
                duchaPrice = 165000.0,
                descalcificadorPrice = 185000.0,
                domusPrice = 1220000.0,
                maximaPrice = 840000.0,
                piletaPrice = 650000.0,
                airePrice = 510000.0,
                ropotPrice = 1080000.0,
                rotechPrice = 1560000.0,
                botellaTermicaPrice = 65000.0,
                repuestoSeniorPrice = 79000.0,
                installmentsCount = 12
            )
            "BO" -> PsaPricingConfig(
                bidonPrice = 20.0,
                bottlePrice = 8.0,
                senior4Price = 5200.0,
                quantumPrice = 6900.0,
                senikPrice = 6900.0,
                cromaPrice = 4300.0,
                veroPrice = 3100.0,
                s1000Price = 5900.0,
                miniPrice = 2400.0,
                c3Price = 1400.0,
                duchaPrice = 1250.0,
                descalcificadorPrice = 1400.0,
                domusPrice = 9200.0,
                maximaPrice = 6200.0,
                piletaPrice = 4900.0,
                airePrice = 3800.0,
                ropotPrice = 8100.0,
                rotechPrice = 11700.0,
                botellaTermicaPrice = 520.0,
                repuestoSeniorPrice = 600.0,
                installmentsCount = 12
            )
            "PY" -> PsaPricingConfig(
                bidonPrice = 20000.0,
                bottlePrice = 8000.0,
                senior4Price = 5200000.0,
                quantumPrice = 6900000.0,
                senikPrice = 6900000.0,
                cromaPrice = 4300000.0,
                veroPrice = 3100000.0,
                s1000Price = 5900000.0,
                miniPrice = 2400000.0,
                c3Price = 1400000.0,
                duchaPrice = 1250000.0,
                descalcificadorPrice = 1400000.0,
                domusPrice = 9200000.0,
                maximaPrice = 6200000.0,
                piletaPrice = 4900000.0,
                airePrice = 3800000.0,
                ropotPrice = 8100000.0,
                rotechPrice = 11700000.0,
                botellaTermicaPrice = 520000.0,
                repuestoSeniorPrice = 600000.0,
                installmentsCount = 12
            )
            "PE" -> PsaPricingConfig(
                bidonPrice = 18.0,
                bottlePrice = 5.0,
                senior4Price = 2600.0,
                quantumPrice = 3500.0,
                senikPrice = 3500.0,
                cromaPrice = 2150.0,
                veroPrice = 1550.0,
                s1000Price = 2950.0,
                miniPrice = 1200.0,
                c3Price = 700.0,
                duchaPrice = 620.0,
                descalcificadorPrice = 700.0,
                domusPrice = 4600.0,
                maximaPrice = 3150.0,
                piletaPrice = 2450.0,
                airePrice = 1900.0,
                ropotPrice = 4100.0,
                rotechPrice = 5900.0,
                botellaTermicaPrice = 260.0,
                repuestoSeniorPrice = 310.0,
                installmentsCount = 12
            )
            "CO" -> PsaPricingConfig(
                bidonPrice = 15000.0,
                bottlePrice = 4500.0,
                senior4Price = 2900000.0,
                quantumPrice = 3900000.0,
                senikPrice = 3900000.0,
                cromaPrice = 2400000.0,
                veroPrice = 1750000.0,
                s1000Price = 3300000.0,
                miniPrice = 1350000.0,
                c3Price = 780000.0,
                duchaPrice = 690000.0,
                descalcificadorPrice = 780000.0,
                domusPrice = 5100000.0,
                maximaPrice = 3500000.0,
                piletaPrice = 2750000.0,
                airePrice = 2150000.0,
                ropotPrice = 4550000.0,
                rotechPrice = 6550000.0,
                botellaTermicaPrice = 290000.0,
                repuestoSeniorPrice = 340000.0,
                installmentsCount = 12
            )
            "EC" -> PsaPricingConfig(
                bidonPrice = 3.0,
                bottlePrice = 1.0,
                senior4Price = 690.0,
                quantumPrice = 940.0,
                senikPrice = 940.0,
                cromaPrice = 570.0,
                veroPrice = 410.0,
                s1000Price = 790.0,
                miniPrice = 320.0,
                c3Price = 185.0,
                duchaPrice = 165.0,
                descalcificadorPrice = 185.0,
                domusPrice = 1220.0,
                maximaPrice = 840.0,
                piletaPrice = 650.0,
                airePrice = 500.0,
                ropotPrice = 1090.0,
                rotechPrice = 1570.0,
                botellaTermicaPrice = 68.0,
                repuestoSeniorPrice = 78.0,
                installmentsCount = 12
            )
            "ES" -> PsaPricingConfig(
                bidonPrice = 5.0,
                bottlePrice = 2.0,
                senior4Price = 650.0,
                quantumPrice = 890.0,
                senikPrice = 890.0,
                cromaPrice = 540.0,
                veroPrice = 390.0,
                s1000Price = 750.0,
                miniPrice = 300.0,
                c3Price = 175.0,
                duchaPrice = 155.0,
                descalcificadorPrice = 175.0,
                domusPrice = 1150.0,
                maximaPrice = 800.0,
                piletaPrice = 610.0,
                airePrice = 470.0,
                ropotPrice = 1020.0,
                rotechPrice = 1480.0,
                botellaTermicaPrice = 62.0,
                repuestoSeniorPrice = 72.0,
                installmentsCount = 12
            )
            else -> defaultPricingForCountry("AR")
        }
    }
}
