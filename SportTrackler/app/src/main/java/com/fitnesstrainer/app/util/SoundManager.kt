package com.fitnesstrainer.app.util

import android.content.Context
import android.media.AudioManager
import android.view.SoundEffectConstants
import android.view.View

object SoundManager {

    private var audioManager: AudioManager? = null

    fun init(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun playClick(view: View) {
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    fun playNav(view: View) {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP)
    }

    fun playSuccess(view: View) {
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
    }

    fun playError() {
        audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_INVALID)
    }

    fun playDelete() {
        audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
    }
}
