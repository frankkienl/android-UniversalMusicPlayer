/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.android.uamp.EMPTY_PLAYBACK_STATE
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.MediaItemFragment
import com.example.android.uamp.MediaSessionConnection
import com.example.android.uamp.NOTHING_PLAYING
import com.example.android.uamp.R
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.isPauseEnabled
import com.example.android.uamp.media.extensions.isPlayEnabled

/**
 * [ViewModel] for [MediaItemFragment].
 */
class MediaItemFragmentViewModel(private val mediaId: String,
                                 mediaSessionConnection: MediaSessionConnection
) : ViewModel() {

    /**
     * Use a backing property so consumers of mediaItems only get a [LiveData] instance so
     * they don't inadvertently modify it.
     */
    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
            .apply { postValue(emptyList()) }
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems

    private val subscriptionCallback = object : SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            val itemsList = children.map { child ->
                MediaItemData(child.mediaId!!,
                        child.description.title.toString(),
                        child.description.subtitle.toString(),
                        child.description.iconUri!!,
                        getResourceForMediaId(child.mediaId!!))
            }
            _mediaItems.postValue(itemsList)
        }
    }

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE
        val metadata = mediaSessionConnection.nowPlaying.value ?: NOTHING_PLAYING
        _mediaItems.postValue(updateState(playbackState, metadata))
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        val playbackState = mediaSessionConnection.playbackState.value ?: EMPTY_PLAYBACK_STATE
        val metadata = it ?: NOTHING_PLAYING
        _mediaItems.postValue(updateState(playbackState, metadata))
    }

    /**
     * Because there's a complex dance between this [ViewModel] and the [MediaSessionConnection]
     * (which is wrapping a [MediaBrowserCompat] object), the usual guidance of using
     * [Transformations] doesn't quite work.
     *
     * Specifically there's three things that are watched that will cause the single piece of
     * [LiveData] exposed from this class to be updated.
     *
     * [subscriptionCallback] (defined above) is called if/when the children of this
     * ViewModel's [mediaId] changes.
     *
     * [MediaSessionConnection.playbackState] changes state based on the playback state of
     * the player, which can change the [MediaItemData.playbackRes]s in the list.
     *
     * [MediaSessionConnection.nowPlaying] changes based on the item that's being played,
     * which can also change the [MediaItemData.playbackRes]s in the list.
     */
    private val mediaSessionConnection = mediaSessionConnection.also {
        it.subscribe(mediaId, subscriptionCallback)

        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
    }

    override fun onCleared() {
        super.onCleared()

        /**
         * When the [ViewModel] is no longer in use, remove the permanent observers from
         * the [MediaSessionConnection] [LiveData] sources.
         */
        mediaSessionConnection.playbackState.removeObserver(playbackStateObserver)
        mediaSessionConnection.nowPlaying.removeObserver(mediaMetadataObserver)

        // And then, finally, unsubscribe the media ID that was being watched.
        mediaSessionConnection.unsubscribe(mediaId, subscriptionCallback)
    }

    /**
     * This method takes a [MediaItemData] and does one of the following:
     * - If the item is *not* the active item, then play it directly.
     * - If the item *is* the active item, check whether "pause" is a permitted command. If it is,
     *   then pause playback, otherwise send "play" to resume playback.
     */
    fun playMedia(mediaItem: MediaItemData) {
        val nowPlaying = mediaSessionConnection.nowPlaying.value
        val transportControls = mediaSessionConnection.transportControls

        if (mediaItem.mediaId == nowPlaying?.id) {
            mediaSessionConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPauseEnabled -> transportControls.pause()
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                " (mediaId=${mediaItem.mediaId})")
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }

    private fun getResourceForMediaId(mediaId: String): Int {
        val isActive = mediaId == mediaSessionConnection.nowPlaying.value?.id
        return if (!isActive) {
            0 // Item isn't active, so don't show a button at all.
        } else {
            val pauseEnabled = mediaSessionConnection.playbackState.value?.isPauseEnabled ?: false
            return if (pauseEnabled) R.drawable.ic_pause_black_24dp
            else R.drawable.ic_play_arrow_black_24dp
        }
    }

    private fun updateState(playbackState: PlaybackStateCompat,
                            mediaMetadata: MediaMetadataCompat): List<MediaItemData> {

        val newResId = when (playbackState.isPauseEnabled) {
            true -> R.drawable.ic_pause_black_24dp
            else -> R.drawable.ic_play_arrow_black_24dp
        }

        return mediaItems.value?.map {
            val useResId = if (it.mediaId == mediaMetadata.id) newResId else 0
            it.copy(playbackRes = useResId)
        } ?: emptyList()
    }

    class Factory(private val mediaId: String,
                  private val mediaSessionConnection: MediaSessionConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MediaItemFragmentViewModel(mediaId, mediaSessionConnection) as T
        }
    }
}

private const val TAG = "MediaItemFragmentVM"
