package com.tuapp.puzzles.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tuapp.puzzles.data.AssetCampaignSource
import com.tuapp.puzzles.data.CampaignRepository
import com.tuapp.puzzles.data.CampaignSource
import com.tuapp.puzzles.data.DataStoreProgressRepository
import com.tuapp.puzzles.data.DayClock
import com.tuapp.puzzles.data.DefaultCampaignRepository
import com.tuapp.puzzles.data.DefaultLevelGenerator
import com.tuapp.puzzles.data.LevelGenerator
import com.tuapp.puzzles.data.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Una sola instancia de DataStore por proceso (requisito de la librería)
private val Context.puzzlesDataStore: DataStore<Preferences> by preferencesDataStore(name = "puzzles_progress")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.puzzlesDataStore

    @Provides
    fun provideDayClock(): DayClock = DayClock { LocalDate.now().toEpochDay() }

    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindProgress(impl: DataStoreProgressRepository): ProgressRepository
    @Binds abstract fun bindCampaign(impl: DefaultCampaignRepository): CampaignRepository
    @Binds abstract fun bindCampaignSource(impl: AssetCampaignSource): CampaignSource
    @Binds abstract fun bindGenerator(impl: DefaultLevelGenerator): LevelGenerator
}
