package com.example.kotlin_holy.ui.shell

import com.example.kotlin_holy.domain.repository.ReadingTimeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O'qish vaqti — veb nusxadagi src/lib/readingTime.js: o'qish sahifalarida (sura, sahifa,
 * juz) har kuni o'tkazilgan vaqt. Ilovada shunchaki ochiq turgan vaqt hisoblanmaydi: vaqt
 * faqat harakatlar (bosish, varaqlash) orasidagi oraliqlardan yig'iladi, oraliq bir
 * daqiqadan uzun bo'lsa — o'qish to'xtagan deb, butunlay tashlab yuboriladi.
 */
@Singleton
class ReadingTracker @Inject constructor(
    private val repository: ReadingTimeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()

    /* Hali saqlanmagan vaqt va joriy seans (so'nggi harakat vaqti) */
    private val pending = HashMap<String, Long>()
    private var lastActivity: Long? = null
    private var saver: Job? = null

    private val _pendingFlow = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Hali yozilmagan vaqt — bugungi ko'rsatkichga qo'shib ko'rsatiladi */
    val unsaved: StateFlow<Map<String, Long>> = _pendingFlow.asStateFlow()

    /** O'qish sahifasi ochildi (yoki ilova oldinga chiqdi) */
    fun start() = synchronized(lock) {
        if (lastActivity == null) lastActivity = System.currentTimeMillis()
        if (saver?.isActive != true) {
            saver = scope.launch {
                while (isActive) {
                    delay(SAVE_EVERY)
                    flush()
                }
            }
        }
    }

    /** O'qish sahifasidan chiqildi yoki ilova yashirindi */
    fun stop() {
        synchronized(lock) {
            settle(System.currentTimeMillis())
            lastActivity = null
            saver?.cancel()
            saver = null
        }
        scope.launch { flush() }
    }

    /** Bosish, varaqlash — harakat bo'ldi */
    fun activity() = synchronized(lock) {
        val last = lastActivity ?: return@synchronized
        val now = System.currentTimeMillis()
        if (now - last < ACTIVITY_THROTTLE) return@synchronized
        settle(now)
    }

    /** Tozalashdan oldingi oraliq hisobga kirmaydi */
    fun resetSession() = synchronized(lock) {
        pending.clear()
        _pendingFlow.value = emptyMap()
        if (lastActivity != null) lastActivity = System.currentTimeMillis()
    }

    /* So'nggi harakatdan shu paytgacha bo'lgan oraliq: bir daqiqadan oshmasa — o'qilgan */
    private fun settle(now: Long) {
        val last = lastActivity ?: return
        if (now - last <= IDLE_LIMIT) credit(last, now)
        lastActivity = now
    }

    /* Oraliqni kunlarga bo'lib qo'shamiz — yarim tundan o'tsa, har kun o'z ulushini oladi */
    private fun credit(from: Long, to: Long) {
        var start = from
        while (start < to) {
            val midnight = Calendar.getInstance().apply {
                timeInMillis = start
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
            val end = minOf(to, midnight)
            val key = dayKey(start)
            pending[key] = (pending[key] ?: 0L) + (end - start)
            start = end
        }
        _pendingFlow.value = HashMap(pending)
    }

    private suspend fun flush() {
        val batch = synchronized(lock) {
            if (pending.isEmpty()) return
            val copy = HashMap(pending)
            pending.clear()
            copy
        }
        repository.add(batch)
        synchronized(lock) { _pendingFlow.value = HashMap(pending) }
    }

    companion object {
        const val IDLE_LIMIT = 60_000L
        const val SAVE_EVERY = 30_000L
        const val ACTIVITY_THROTTLE = 1_000L

        /** Mahalliy sana kaliti: "2026-10-06" */
        fun dayKey(time: Long): String {
            val c = Calendar.getInstance().apply { timeInMillis = time }
            fun two(v: Int) = v.toString().padStart(2, '0')
            return "${c.get(Calendar.YEAR)}-${two(c.get(Calendar.MONTH) + 1)}-${two(c.get(Calendar.DAY_OF_MONTH))}"
        }

        /** 4 980 000 ms → "1 soat 23 daqiqa"; short bilan → "1 soat 23 daq" */
        fun formatDuration(ms: Long, short: Boolean = false): String {
            val minutes = (ms / 60_000).toInt()
            val hours = minutes / 60
            val rest = minutes % 60
            val unit = if (short) "daq" else "daqiqa"
            return when {
                hours == 0 -> "$rest $unit"
                rest == 0 -> "$hours soat"
                else -> "$hours soat $rest $unit"
            }
        }
    }
}
