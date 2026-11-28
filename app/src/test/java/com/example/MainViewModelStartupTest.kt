package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.FirestoreTeamRepository
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelStartupTest {

    @Test
    fun `MainViewModel instantiates cleanly when FirebaseApp is not initialized`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        assertFalse(viewModel.isCloudGoogleConnected.value)

        // Foreground & update hooks should not throw when Firebase is missing
        viewModel.onAppForegrounded(app)
        viewModel.trySilentCloudSignIn(app)
        viewModel.checkForAppUpdate(isAutomaticStartup = true)
        viewModel.recheckCloudAccessNow(silentIfNotLocked = true)
    }

    @Test
    fun `FirestoreTeamRepository auth and observers fail gracefully without FirebaseApp`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = FirestoreTeamRepository(app)

        assertFalse(repo.isFirebaseInitialized())
        assertNull(repo.getCurrentFirebaseUser())
        assertEquals("", repo.currentOwnerUid())
        assertNull(repo.trySilentGoogleSignIn(app))
        assertNull(repo.fetchAppVersionFromServer())
        assertEquals(0, repo.flushPendingAppErrors())

        // Observers must terminate cleanly without throwing IllegalStateException
        assertEquals(emptyList<Any>(), repo.observeUserDocument("vendedor@psa.test").toList())
        assertEquals(listOf(emptyList<Any>()), repo.observeAllAuthorizedUsers().toList())
        assertEquals(emptyList<Any>(), repo.observeRoleMatrix().toList())
        assertEquals(emptyList<Any>(), repo.observeGlobalPsaPricingSync().toList())
        assertEquals(listOf(emptyList<Any>()), repo.observeTeamNotificationsForUser("vendedor@psa.test", false).toList())
        assertEquals(listOf(emptyList<Any>()), repo.observeMensajesGlobales(false).toList())
    }
}
