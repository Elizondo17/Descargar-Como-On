package com.example.util

import android.util.Log
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class FirebaseDistributionReleaseSummary(
    val versionCode: Long,
    val displayVersion: String,
    val releaseNotes: String
)

sealed class FirebaseAppDistributionOutcome {
    data class UpdateAvailablePrompted(val release: FirebaseDistributionReleaseSummary?) : FirebaseAppDistributionOutcome()
    data object UpToDate : FirebaseAppDistributionOutcome()
    data object NotSignedIn : FirebaseAppDistributionOutcome()
    data object NotImplementedInReleaseBuild : FirebaseAppDistributionOutcome()
    data class Failed(val message: String) : FirebaseAppDistributionOutcome()
}

object FirebaseAppDistributionHelper {
    private const val TAG = "FirebaseAppDistHelper"

    fun isTesterSignedIn(): Boolean {
        return runCatching {
            FirebaseAppDistribution.getInstance().isTesterSignedIn
        }.getOrDefault(false)
    }

    suspend fun checkAndPromptInAppUpdate(
        interactiveSignInIfNeeded: Boolean
    ): FirebaseAppDistributionOutcome {
        return try {
            val appDistribution = FirebaseAppDistribution.getInstance()
            if (!appDistribution.isTesterSignedIn) {
                if (!interactiveSignInIfNeeded) {
                    return FirebaseAppDistributionOutcome.NotSignedIn
                }
                appDistribution.signInTester().await()
            }

            val release = appDistribution.checkForNewRelease().await()
            if (release == null) {
                return FirebaseAppDistributionOutcome.UpToDate
            }

            val summary = FirebaseDistributionReleaseSummary(
                versionCode = release.versionCode,
                displayVersion = release.displayVersion,
                releaseNotes = release.releaseNotes.orEmpty()
            )
            appDistribution.updateIfNewReleaseAvailable().await()
            FirebaseAppDistributionOutcome.UpdateAvailablePrompted(summary)
        } catch (ce: CancellationException) {
            throw ce
        } catch (fex: FirebaseAppDistributionException) {
            when (fex.errorCode) {
                FirebaseAppDistributionException.Status.NOT_IMPLEMENTED ->
                    FirebaseAppDistributionOutcome.NotImplementedInReleaseBuild
                FirebaseAppDistributionException.Status.AUTHENTICATION_CANCELED ->
                    FirebaseAppDistributionOutcome.Failed("Inicio de sesión como tester cancelado.")
                FirebaseAppDistributionException.Status.AUTHENTICATION_FAILURE ->
                    FirebaseAppDistributionOutcome.Failed("Tu correo aún no está invitado en Firebase App Distribution.")
                FirebaseAppDistributionException.Status.NETWORK_FAILURE ->
                    FirebaseAppDistributionOutcome.Failed("Sin conexión estable para consultar Firebase App Distribution.")
                else -> {
                    Log.w(TAG, "Firebase App Distribution status=${fex.errorCode}: ${fex.message}")
                    FirebaseAppDistributionOutcome.Failed(
                        fex.localizedMessage ?: "No se pudo verificar actualizaciones en Firebase App Distribution."
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase App Distribution error: ${e.message}")
            FirebaseAppDistributionOutcome.Failed(
                e.localizedMessage ?: "No se pudo verificar Firebase App Distribution."
            )
        }
    }
}
