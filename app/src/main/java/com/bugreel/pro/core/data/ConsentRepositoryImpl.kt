package com.bugreel.pro.core.data

import androidx.datastore.core.DataStore
import com.bugreel.pro.core.data.local.onboarding.ConsentPreferences
import com.bugreel.pro.core.domain.model.ConsentState
import com.bugreel.pro.core.domain.repository.ConsentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConsentRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<ConsentPreferences>
) : ConsentRepository {

    override val consentState: Flow<ConsentState> = dataStore.data.map {
        ConsentState(consentGiven = it.consentGiven, aiTrainingOptIn = it.aiTrainingOptIn)
    }

    override suspend fun setConsentGiven(given: Boolean) {
        dataStore.updateData { it.copy(consentGiven = given) }
    }

    override suspend fun setAiTrainingOptIn(optedIn: Boolean) {
        dataStore.updateData { it.copy(aiTrainingOptIn = optedIn) }
    }
}
