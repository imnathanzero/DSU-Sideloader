package git.imnathanzero.sysphere.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import git.imnathanzero.sysphere.core.StorageManager
import git.imnathanzero.sysphere.model.Session
import git.imnathanzero.sysphere.preferences.AppPrefs
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@InstallIn(SingletonComponent::class)
@Module
object DataModules {

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() },
            ),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { appContext.preferencesDataStoreFile(AppPrefs.USER_PREFERENCES) },
        )
    }

    @Singleton
    @Provides
    fun providesStorageManager(
        @ApplicationContext appContext: Context,
        preferences: DataStore<Preferences>,
    ): StorageManager {
        return StorageManager(appContext, preferences)
    }

    @Singleton
    @Provides
    fun provideSession(): Session {
        return Session()
    }
}
