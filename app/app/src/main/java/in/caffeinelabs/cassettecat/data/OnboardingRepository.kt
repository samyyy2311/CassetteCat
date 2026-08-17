package `in`.caffeinelabs.cassettecat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")
private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

class OnboardingRepository(private val context: Context) {
    val onboardingCompleted: Flow<Boolean> =
        context.onboardingDataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.onboardingDataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }
}
