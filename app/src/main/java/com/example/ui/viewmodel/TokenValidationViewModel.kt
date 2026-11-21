package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.UserToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface TokenValidationState {
    object Idle : TokenValidationState
    object Validating : TokenValidationState
    data class Valid(val token: UserToken, val message: String) : TokenValidationState
    data class Expired(val token: UserToken, val daysGranted: Int, val expiredDateFormatted: String) : TokenValidationState
    data class Blocked(val token: UserToken, val message: String) : TokenValidationState
    data class Invalid(val error: String) : TokenValidationState
}

class TokenValidationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val tokenDao = database.userTokenDao()

    private val _validationState = MutableStateFlow<TokenValidationState>(TokenValidationState.Idle)
    val validationState: StateFlow<TokenValidationState> = _validationState.asStateFlow()

    private val _activeToken = MutableStateFlow<UserToken?>(null)
    val activeToken: StateFlow<UserToken?> = _activeToken.asStateFlow()

    val allTokens: StateFlow<List<UserToken>> = tokenDao.getAllTokens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secondaryTokens: StateFlow<List<UserToken>> = tokenDao.getSecondaryUserTokens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedDefaultTokensIfEmpty()
    }

    private fun seedDefaultTokensIfEmpty() {
        viewModelScope.launch {
            val superuserToken = tokenDao.getSuperuserToken()
            if (superuserToken == null) {
                val now = System.currentTimeMillis()
                val masterToken = UserToken(
                    token = "SUPER-ADMIN-2026",
                    userId = "admin-master",
                    username = "Super Administrador",
                    role = "SUPERUSER",
                    validFromMs = now,
                    expiresAtMs = now + (1000L * 60 * 60 * 24 * 365 * 10), // 10 years
                    daysGranted = 3650,
                    isBlocked = false,
                    notes = "Llave maestra con privilegios completos de administración"
                )
                tokenDao.insertToken(masterToken)

                val demoOperatorToken = UserToken(
                    token = "EZO-OP-7789-V15D",
                    userId = "op-elena",
                    username = "Operador Ventas 1",
                    role = "OPERATOR",
                    validFromMs = now,
                    expiresAtMs = now + (1000L * 60 * 60 * 24 * 15), // 15 days
                    daysGranted = 15,
                    isBlocked = false,
                    notes = "Operador para envíos en EzoCRM"
                )
                tokenDao.insertToken(demoOperatorToken)
            }
        }
    }

    /**
     * Validates an input token against expiration date, active state, and role.
     */
    fun validateToken(tokenInput: String, onComplete: ((TokenValidationState) -> Unit)? = null) {
        val trimmed = tokenInput.trim()
        if (trimmed.isBlank()) {
            val state = TokenValidationState.Invalid("El token de acceso no puede estar vacío.")
            _validationState.value = state
            onComplete?.invoke(state)
            return
        }

        _validationState.value = TokenValidationState.Validating

        viewModelScope.launch {
            val foundToken = tokenDao.findByToken(trimmed)
            if (foundToken == null) {
                val state = TokenValidationState.Invalid("Token no encontrado o código erróneo.")
                _validationState.value = state
                onComplete?.invoke(state)
                return@launch
            }

            // Check if blocked by superuser
            if (foundToken.isBlocked) {
                val state = TokenValidationState.Blocked(
                    token = foundToken,
                    message = "El acceso para este token ha sido bloqueado por el Superusuario."
                )
                _validationState.value = state
                _activeToken.value = null
                onComplete?.invoke(state)
                return@launch
            }

            // Check expiration date
            if (foundToken.isExpired) {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val expiredStr = formatter.format(Date(foundToken.expiresAtMs))
                val state = TokenValidationState.Expired(
                    token = foundToken,
                    daysGranted = foundToken.daysGranted,
                    expiredDateFormatted = expiredStr
                )
                _validationState.value = state
                _activeToken.value = null
                onComplete?.invoke(state)
                return@launch
            }

            // Token is active and valid
            val now = System.currentTimeMillis()
            tokenDao.updateLastValidated(trimmed, now)
            val updatedToken = foundToken.copy(lastValidatedAt = now)

            _activeToken.value = updatedToken
            val state = TokenValidationState.Valid(
                token = updatedToken,
                message = "Acceso concedido para ${updatedToken.username} (${updatedToken.remainingDays} días restantes)"
            )
            _validationState.value = state
            onComplete?.invoke(state)
        }
    }

    /**
     * Superuser Action: Create a secondary user token with assigned number of days
     */
    fun createOperatorToken(
        name: String,
        days: Int,
        notes: String,
        onSuccess: (UserToken) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            onError("Debes ingresar un nombre de operador.")
            return
        }
        if (days <= 0) {
            onError("Los días de vigencia deben ser mayor a 0.")
            return
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val durationMs = days.toLong() * 24L * 60L * 60L * 1000L
                val tokenString = "EZO-" + cleanName.take(3).uppercase() + "-" +
                        UUID.randomUUID().toString().substring(0, 4).uppercase() + "-D${days}"

                val newToken = UserToken(
                    token = tokenString,
                    userId = "user-" + UUID.randomUUID().toString().substring(0, 8),
                    username = cleanName,
                    role = "OPERATOR",
                    validFromMs = now,
                    expiresAtMs = now + durationMs,
                    daysGranted = days,
                    isBlocked = false,
                    createdAt = now,
                    lastValidatedAt = now,
                    notes = notes.trim()
                )

                tokenDao.insertToken(newToken)
                onSuccess(newToken)
            } catch (e: Exception) {
                onError(e.message ?: "Error al generar el token")
            }
        }
    }

    /**
     * Superuser Action: Extend token expiration by adding more days
     */
    fun addDaysToToken(tokenString: String, extraDays: Int) {
        if (extraDays <= 0) return
        val extraMillis = extraDays.toLong() * 24L * 60L * 60L * 1000L
        viewModelScope.launch {
            tokenDao.extendTokenDays(tokenString, extraMillis, extraDays)
            // If currently validated, refresh it
            if (_activeToken.value?.token == tokenString) {
                _activeToken.value = tokenDao.findByToken(tokenString)
            }
        }
    }

    /**
     * Superuser Action: Block or unblock a token immediately
     */
    fun toggleBlockToken(tokenString: String, shouldBlock: Boolean) {
        viewModelScope.launch {
            tokenDao.setTokenBlocked(tokenString, shouldBlock)
            if (_activeToken.value?.token == tokenString) {
                _activeToken.value = tokenDao.findByToken(tokenString)
            }
        }
    }

    /**
     * Superuser Action: Remove a token permanently
     */
    fun deleteToken(userToken: UserToken) {
        viewModelScope.launch {
            tokenDao.deleteToken(userToken)
            if (_activeToken.value?.token == userToken.token) {
                _activeToken.value = null
                _validationState.value = TokenValidationState.Idle
            }
        }
    }

    fun logout() {
        _activeToken.value = null
        _validationState.value = TokenValidationState.Idle
    }
}
