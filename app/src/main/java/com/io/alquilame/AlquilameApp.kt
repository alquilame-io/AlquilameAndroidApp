package com.io.alquilame

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.io.alquilame.data.Backend
import com.io.alquilame.data.SessionStore

class AlquilameApp : Application(), SingletonImageLoader.Factory {
    val session: SessionStore by lazy { SessionStore(this) }
    val backend: Backend by lazy { Backend(this, session) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).crossfade(true).build()
}
