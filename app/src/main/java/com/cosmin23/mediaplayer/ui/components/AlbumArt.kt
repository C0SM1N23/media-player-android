package com.cosmin23.mediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmin23.mediaplayer.R
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.artwork

/**
 * Album artwork with a themed music-note placeholder. The [model] is resolved by the custom Coil
 * fetcher (album-art uri → system thumbnail → embedded picture), so art appears reliably and
 * failed loads simply reveal the placeholder underneath.
 */
@Composable
fun AlbumArt(
    model: Any?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val placeholderBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(placeholderBrush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxSize(0.4f)
        )
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cd_album_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Overload for the common case of rendering a track's artwork. */
@Composable
fun AlbumArt(
    item: AudioItem,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) = AlbumArt(model = item.artwork, modifier = modifier, cornerRadius = cornerRadius)
