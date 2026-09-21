package com.example.kotlin_holy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kotlin_holy.domain.model.MEMORIZATION_TARGET
import com.example.kotlin_holy.domain.model.MemorizationProgress
import com.example.kotlin_holy.domain.repository.AppSettings
import com.example.kotlin_holy.domain.repository.DEFAULT_RECITER_ID
import com.example.kotlin_holy.domain.repository.MemorizationRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("holy_settings")
private val Context.xatmStore: DataStore<Preferences> by preferencesDataStore("holy_xatm")
private val Context.memorizationStore: DataStore<Preferences> by preferencesDataStore("holy_memorization")

/** Sozlamalar DataStore'da saqlanadi — ilova yopilsa ham qoladi */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
) : SettingsRepository {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val arabicScale = floatPreferencesKey("arabic_scale")
        val translationScale = floatPreferencesKey("translation_scale")
        val textMode = stringPreferencesKey("text_mode")
        val viewMode = stringPreferencesKey("view_mode")
        val surahOrder = stringPreferencesKey("surah_order")
        val pageOrder = stringPreferencesKey("page_order")
        val reciter = intPreferencesKey("reciter_id")
        val lastReadRoute = stringPreferencesKey("last_read_route")
        val lastReadLabel = stringPreferencesKey("last_read_label")
    }

    override val settings: Flow<AppSettings> = context.settingsStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            AppSettings(
                themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.theme] ?: "SYSTEM") }
                    .getOrDefault(ThemeMode.SYSTEM),
                arabicScale = prefs[Keys.arabicScale] ?: 1f,
                translationScale = prefs[Keys.translationScale] ?: 1f,
                textMode = prefs[Keys.textMode] ?: "both",
                viewMode = prefs[Keys.viewMode] ?: "ayah",
                surahOrder = prefs[Keys.surahOrder] ?: "asc",
                pageOrder = prefs[Keys.pageOrder] ?: "asc",
                reciterId = prefs[Keys.reciter] ?: DEFAULT_RECITER_ID,
                lastReadRoute = prefs[Keys.lastReadRoute],
                lastReadLabel = prefs[Keys.lastReadLabel],
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }

    override suspend fun setArabicScale(scale: Float) = edit { it[Keys.arabicScale] = scale }

    override suspend fun setTranslationScale(scale: Float) =
        edit { it[Keys.translationScale] = scale }

    override suspend fun setTextMode(mode: String) = edit { it[Keys.textMode] = mode }

    override suspend fun setViewMode(mode: String) = edit { it[Keys.viewMode] = mode }

    override suspend fun setSurahOrder(order: String) = edit { it[Keys.surahOrder] = order }

    override suspend fun setPageOrder(order: String) = edit { it[Keys.pageOrder] = order }

    override suspend fun setReciter(id: Int) = edit { it[Keys.reciter] = id }

    override suspend fun setLastRead(route: String?, label: String?) = edit { prefs ->
        if (route == null || label == null) {
            prefs.remove(Keys.lastReadRoute)
            prefs.remove(Keys.lastReadLabel)
        } else {
            prefs[Keys.lastReadRoute] = route
            prefs[Keys.lastReadLabel] = label
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit(block)
    }
}

/**
 * Xatm belgilari: har bir o'qilgan bet uchun "page_293" kaliti ostida
 * belgilangan vaqt saqlanadi.
 */
@Singleton
class XatmRepositoryImpl @Inject constructor(
    private val context: Context,
) : XatmRepository {

    private fun key(page: Int) = longPreferencesKey("page_$page")

    override val readPages: Flow<Map<Int, Long>> = context.xatmStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            buildMap {
                prefs.asMap().forEach { (key, value) ->
                    val page = key.name.removePrefix("page_").toIntOrNull()
                    if (page != null && value is Long) put(page, value)
                }
            }
        }

    override suspend fun toggle(page: Int) {
        context.xatmStore.edit { prefs ->
            if (prefs[key(page)] != null) prefs.remove(key(page))
            else prefs[key(page)] = System.currentTimeMillis()
        }
    }

    override suspend fun setRange(from: Int, to: Int, read: Boolean, keepExisting: Boolean) {
        val now = System.currentTimeMillis()
        context.xatmStore.edit { prefs ->
            for (page in from..to) {
                when {
                    !read -> prefs.remove(key(page))
                    keepExisting && prefs[key(page)] != null -> Unit
                    else -> prefs[key(page)] = now
                }
            }
        }
    }

    override suspend fun reset() {
        context.xatmStore.edit { it.clear() }
    }
}

/**
 * Yodlash jarayoni: har bir oyat uchun "streak_<verseKey>" ostida ketma-ket
 * to'g'ri o'qishlar soni, [MEMORIZATION_TARGET]ga yetganda esa
 * "memorized_<verseKey>" ostida belgilangan vaqt saqlanadi.
 */
@Singleton
class MemorizationRepositoryImpl @Inject constructor(
    private val context: Context,
) : MemorizationRepository {

    private fun streakKey(verseKey: String) = intPreferencesKey("streak_$verseKey")
    private fun memorizedKey(verseKey: String) = longPreferencesKey("memorized_$verseKey")

    override val progress: Flow<Map<String, MemorizationProgress>> = context.memorizationStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val streaks = mutableMapOf<String, Int>()
            val memorizedAt = mutableMapOf<String, Long>()
            prefs.asMap().forEach { (key, value) ->
                when {
                    key.name.startsWith("streak_") && value is Int ->
                        streaks[key.name.removePrefix("streak_")] = value
                    key.name.startsWith("memorized_") && value is Long ->
                        memorizedAt[key.name.removePrefix("memorized_")] = value
                }
            }
            (streaks.keys + memorizedAt.keys).associateWith { verseKey ->
                MemorizationProgress(
                    verseKey = verseKey,
                    correctStreak = streaks[verseKey] ?: 0,
                    memorizedAt = memorizedAt[verseKey],
                )
            }
        }

    override suspend fun recordAttempt(verseKey: String, correct: Boolean) {
        context.memorizationStore.edit { prefs ->
            if (!correct) {
                prefs.remove(streakKey(verseKey))
                return@edit
            }
            val next = (prefs[streakKey(verseKey)] ?: 0) + 1
            if (next >= MEMORIZATION_TARGET) {
                prefs[memorizedKey(verseKey)] = System.currentTimeMillis()
                prefs.remove(streakKey(verseKey))
            } else {
                prefs[streakKey(verseKey)] = next
            }
        }
    }

    override suspend fun reset(verseKey: String) {
        context.memorizationStore.edit { prefs ->
            prefs.remove(streakKey(verseKey))
            prefs.remove(memorizedKey(verseKey))
        }
    }
}

