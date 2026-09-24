package com.example.kotlin_holy.audio

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Qiroat shu xizmatda ijro etiladi.
 *
 * Ijroni Activity emas, xizmat olib boradi: shunda ekran aylanganda yoki
 * foydalanuvchi boshqa ilovaga o'tganda qiroat uzilmaydi, bildirishnomadagi
 * tugmalardan boshqariladi va quloqchin uzilsa o'zi to'xtaydi.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            /* Quloqchin uzilganda qiroat davom etib ketmasligi uchun */
            .setHandleAudioBecomingNoisy(true)
            .build()

        session = MediaSession.Builder(this, player).build()
        activePlayer = player
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Foydalanuvchi ilovani yopsa va qiroat to'xtagan bo'lsa, xizmat ham yopiladi */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        activePlayer = null
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        /**
         * Xizmat ilova bilan bitta jarayonda ishlaydi, shuning uchun so'z
         * kuzatuvi pozitsiyani shu pleyerning o'zidan aniq oladi.
         * MediaController esa pozitsiyani faqat taxmin qiladi va uzoq
         * ijroda ("Davomi") bu taxmin eskirib, so'zlar yonmay qoladi.
         */
        var activePlayer: Player? = null
            private set
    }
}
