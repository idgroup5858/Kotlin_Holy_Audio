package com.example.kotlin_holy.di

import android.content.Context
import com.example.kotlin_holy.data.audio.AudioRepositoryImpl
import com.example.kotlin_holy.data.prefs.MarksRepositoryImpl
import com.example.kotlin_holy.data.prefs.ReadingTimeRepositoryImpl
import com.example.kotlin_holy.data.prefs.RemindersRepositoryImpl
import com.example.kotlin_holy.data.prefs.SettingsRepositoryImpl
import com.example.kotlin_holy.data.prefs.XatmRepositoryImpl
import com.example.kotlin_holy.data.repository.LibraryRepositoryImpl
import com.example.kotlin_holy.data.repository.QuranRepositoryImpl
import com.example.kotlin_holy.domain.repository.AudioRepository
import com.example.kotlin_holy.domain.repository.LibraryRepository
import com.example.kotlin_holy.domain.repository.MarksRepository
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.ReadingTimeRepository
import com.example.kotlin_holy.domain.repository.RemindersRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repozitoriy interfeyslari o'z amalga oshirilishiga bog'lanadi */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuranRepository(impl: QuranRepositoryImpl): QuranRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindXatmRepository(impl: XatmRepositoryImpl): XatmRepository

    @Binds
    @Singleton
    abstract fun bindMarksRepository(impl: MarksRepositoryImpl): MarksRepository

    @Binds
    @Singleton
    abstract fun bindRemindersRepository(impl: RemindersRepositoryImpl): RemindersRepository

    @Binds
    @Singleton
    abstract fun bindReadingTimeRepository(impl: ReadingTimeRepositoryImpl): ReadingTimeRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAudioRepository(impl: AudioRepositoryImpl): AudioRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
