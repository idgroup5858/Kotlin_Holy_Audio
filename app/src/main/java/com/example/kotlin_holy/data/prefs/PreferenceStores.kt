package com.example.kotlin_holy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kotlin_holy.data.audio.ReciterCatalog
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.model.numberLabel
import com.example.kotlin_holy.domain.repository.AppSettings
import com.example.kotlin_holy.domain.repository.DEFAULT_RECITER_ID
import com.example.kotlin_holy.domain.repository.MarksRepository
import com.example.kotlin_holy.domain.repository.ReadingTimeRepository
import com.example.kotlin_holy.domain.repository.RemindersRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.theme.Appearance
import com.example.kotlin_holy.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("holy_settings")
private val Context.xatmStore: DataStore<Preferences> by preferencesDataStore("holy_xatm")
private val Context.marksStore: DataStore<Preferences> by preferencesDataStore("holy_marks")
private val Context.remindersStore: DataStore<Preferences> by preferencesDataStore("holy_reminders")
private val Context.readingStore: DataStore<Preferences> by preferencesDataStore("holy_reading_time")

private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

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
        val listView = stringPreferencesKey("list_view")
        val surahOrder = stringPreferencesKey("surah_order")
        val pageOrder = stringPreferencesKey("page_order")
        val reciter = intPreferencesKey("reciter_id")
        val lastReadRoute = stringPreferencesKey("last_read_route_v2")
        val lastReadLabel = stringPreferencesKey("last_read_label_v2")

        /* Mavzu (Sozlamalar → Mavzu) */
        val apTheme = stringPreferencesKey("appearance_theme")
        val apAccent = stringPreferencesKey("appearance_accent")
        val apPattern = stringPreferencesKey("appearance_pattern")
        val apStrength = stringPreferencesKey("appearance_strength")
        val apSize = stringPreferencesKey("appearance_size")
        val apTint = booleanPreferencesKey("appearance_tint")
        val apGlow = booleanPreferencesKey("appearance_glow")
        val apCards = stringPreferencesKey("appearance_cards")
        val apRadius = stringPreferencesKey("appearance_radius")
    }

    override val settings: Flow<AppSettings> = context.settingsStore.safeData().map { prefs ->
        val defaults = Appearance()
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.theme] ?: "DARK") }
                .getOrDefault(ThemeMode.DARK),
            appearance = Appearance(
                theme = prefs[Keys.apTheme] ?: defaults.theme,
                accent = prefs[Keys.apAccent] ?: defaults.accent,
                pattern = prefs[Keys.apPattern] ?: defaults.pattern,
                strength = prefs[Keys.apStrength] ?: defaults.strength,
                size = prefs[Keys.apSize] ?: defaults.size,
                tint = prefs[Keys.apTint] ?: defaults.tint,
                glow = prefs[Keys.apGlow] ?: defaults.glow,
                cards = prefs[Keys.apCards] ?: defaults.cards,
                radius = prefs[Keys.apRadius] ?: defaults.radius,
            ),
            arabicScale = prefs[Keys.arabicScale] ?: 1f,
            translationScale = prefs[Keys.translationScale] ?: 1f,
            textMode = prefs[Keys.textMode] ?: "both",
            viewMode = prefs[Keys.viewMode] ?: "ayah",
            listView = if (prefs[Keys.listView] == "list") "list" else "grid",
            surahOrder = prefs[Keys.surahOrder] ?: "asc",
            pageOrder = prefs[Keys.pageOrder] ?: "asc",
            /* Ro'yxatdan chiqarilgan (audiosi to'liq bo'lmagan) qori saqlangan bo'lsa — standart qori */
            reciterId = prefs[Keys.reciter]?.takeIf { ReciterCatalog.isAvailable(it) } ?: DEFAULT_RECITER_ID,
            lastReadRoute = prefs[Keys.lastReadRoute],
            lastReadLabel = prefs[Keys.lastReadLabel],
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }

    override suspend fun setAppearance(appearance: Appearance) = edit { prefs ->
        prefs[Keys.apTheme] = appearance.theme
        prefs[Keys.apAccent] = appearance.accent
        prefs[Keys.apPattern] = appearance.pattern
        prefs[Keys.apStrength] = appearance.strength
        prefs[Keys.apSize] = appearance.size
        prefs[Keys.apTint] = appearance.tint
        prefs[Keys.apGlow] = appearance.glow
        prefs[Keys.apCards] = appearance.cards
        prefs[Keys.apRadius] = appearance.radius
    }

    override suspend fun setArabicScale(scale: Float) = edit { it[Keys.arabicScale] = scale }

    override suspend fun setTranslationScale(scale: Float) = edit { it[Keys.translationScale] = scale }

    override suspend fun setTextMode(mode: String) = edit { it[Keys.textMode] = mode }

    override suspend fun setViewMode(mode: String) = edit { it[Keys.viewMode] = mode }

    override suspend fun setListView(view: String) = edit { it[Keys.listView] = view }

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

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsStore.edit(block)
    }
}

/**
 * Xatm belgilari: har bir o'qilgan bet uchun "page_293" kaliti ostida
 * belgilangan vaqt saqlanadi; "started_at" — xatm boshlangan vaqt.
 */
@Singleton
class XatmRepositoryImpl @Inject constructor(
    private val context: Context,
) : XatmRepository {

    private fun key(page: Int) = longPreferencesKey("page_$page")
    private val startedKey = longPreferencesKey("started_at")

    override val readPages: Flow<Map<Int, Long>> = context.xatmStore.safeData().map { prefs ->
        buildMap {
            prefs.asMap().forEach { (key, value) ->
                val page = key.name.removePrefix("page_").takeIf { key.name.startsWith("page_") }?.toIntOrNull()
                if (page != null && value is Long) put(page, value)
            }
        }
    }

    /** Saqlangan vaqt, u bo'lmasa (eski belgilar uchun) eng birinchi belgi */
    override val startedAt: Flow<Long?> = context.xatmStore.safeData().map { prefs ->
        val pages = prefs.asMap().filterKeys { it.name.startsWith("page_") }.values.filterIsInstance<Long>()
        if (pages.isEmpty()) null else prefs[startedKey] ?: pages.min()
    }

    private fun MutablePreferences.syncStart(now: Long) {
        val pages = asMap().filterKeys { it.name.startsWith("page_") }.values.filterIsInstance<Long>()
        if (pages.isEmpty()) remove(startedKey)
        else if (this[startedKey] == null) this[startedKey] = minOf(pages.min(), now)
    }

    override suspend fun toggle(page: Int) {
        val now = System.currentTimeMillis()
        context.xatmStore.edit { prefs ->
            if (prefs[key(page)] != null) prefs.remove(key(page)) else prefs[key(page)] = now
            prefs.syncStart(now)
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
            prefs.syncStart(now)
        }
    }

    override suspend fun reset() {
        context.xatmStore.edit { it.clear() }
    }
}

/**
 * «O'qildi» belgilari: "hadith_bukhari:12" va "book_hikam:12" kalitlari ostida vaqt.
 * Xatm va statistikaga qo'shilmaydi — faqat belgi.
 */
@Singleton
class MarksRepositoryImpl @Inject constructor(
    private val context: Context,
) : MarksRepository {

    private fun marks(prefix: String): Flow<Map<String, Long>> = context.marksStore.safeData().map { prefs ->
        buildMap {
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith(prefix) && value is Long) put(key.name.removePrefix(prefix), value)
            }
        }
    }

    override val hadithMarks: Flow<Map<String, Long>> = marks(HADITH)
    override val bookMarks: Flow<Map<String, Long>> = marks(BOOK)

    private fun hadithKey(collectionId: String, number: Double) =
        longPreferencesKey("$HADITH$collectionId:${numberLabel(number)}")

    override suspend fun toggleHadith(collectionId: String, number: Double) {
        val key = hadithKey(collectionId, number)
        context.marksStore.edit { prefs ->
            if (prefs[key] != null) prefs.remove(key) else prefs[key] = System.currentTimeMillis()
        }
    }

    override suspend fun setHadiths(collectionId: String, numbers: List<Double>, read: Boolean) {
        val now = System.currentTimeMillis()
        context.marksStore.edit { prefs ->
            numbers.forEach { number ->
                val key = hadithKey(collectionId, number)
                if (!read) prefs.remove(key) else if (prefs[key] == null) prefs[key] = now
            }
        }
    }

    override suspend fun toggleBookItem(bookId: String, number: Int) {
        val key = longPreferencesKey("$BOOK$bookId:$number")
        context.marksStore.edit { prefs ->
            if (prefs[key] != null) prefs.remove(key) else prefs[key] = System.currentTimeMillis()
        }
    }

    private companion object {
        const val HADITH = "hadith_"
        const val BOOK = "book_"
    }
}

/** Eslatmalar bitta JSON qatorda saqlanadi: [{ id, type, title, text, source?, createdAt }] */
@Singleton
class RemindersRepositoryImpl @Inject constructor(
    private val context: Context,
) : RemindersRepository {

    private val key = stringPreferencesKey("reminders")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val serializer = ListSerializer(Reminder.serializer())

    private fun decode(raw: String?): List<Reminder> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            ?.filter { it.id.isNotBlank() && it.text.isNotBlank() }
            .orEmpty()

    override val reminders: Flow<List<Reminder>> = context.remindersStore.safeData().map { decode(it[key]) }

    override suspend fun add(type: String, title: String, text: String, source: String?) {
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            id = "${now.toString(36)}-${Random.nextLong(0, 36L * 36 * 36 * 36 * 36 * 36).toString(36)}",
            type = type,
            title = title.trim(),
            text = text.trim(),
            source = source,
            createdAt = now,
        )
        context.remindersStore.edit { prefs ->
            prefs[key] = json.encodeToString(serializer, listOf(reminder) + decode(prefs[key]))
        }
    }

    override suspend fun delete(id: String) {
        context.remindersStore.edit { prefs ->
            prefs[key] = json.encodeToString(serializer, decode(prefs[key]).filter { it.id != id })
        }
    }
}

/** O'qish vaqti: "day_2026-10-06" kaliti ostida ms */
@Singleton
class ReadingTimeRepositoryImpl @Inject constructor(
    private val context: Context,
) : ReadingTimeRepository {

    override val totals: Flow<Map<String, Long>> = context.readingStore.safeData().map { prefs ->
        buildMap {
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("day_") && value is Long && value > 0) put(key.name.removePrefix("day_"), value)
            }
        }
    }

    override suspend fun add(amounts: Map<String, Long>) {
        if (amounts.isEmpty()) return
        context.readingStore.edit { prefs ->
            amounts.forEach { (day, ms) ->
                val key = longPreferencesKey("day_$day")
                prefs[key] = (prefs[key] ?: 0L) + ms
            }
        }
    }

    override suspend fun reset() {
        context.readingStore.edit { it.clear() }
    }
}
