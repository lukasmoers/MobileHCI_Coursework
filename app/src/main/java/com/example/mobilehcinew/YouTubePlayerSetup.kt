package com.example.mobilehcinew

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class YouTubePlayerSetup(private val youTubePlayerView: YouTubePlayerView) {

    lateinit var youTubePlayer: YouTubePlayer
    private val videoId = "S0Q4gqBUs7c"

    fun initializeYouTubePlayer() {
        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@YouTubePlayerSetup.youTubePlayer = youTubePlayer
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })
    }
}
