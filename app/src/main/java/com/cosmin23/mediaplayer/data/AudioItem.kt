package com.cosmin23.mediaplayer.data

import android.net.Uri

data class AudioItem(
    val id: Long,
    val title: String,
    val uri: Uri
)
