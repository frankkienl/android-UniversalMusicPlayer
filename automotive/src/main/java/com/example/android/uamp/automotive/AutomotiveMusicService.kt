/*
 * Copyright 2019 Google Inc. All rights reserved.
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

package com.example.android.uamp.automotive

import android.accounts.AccountManager
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.edit
import com.example.android.uamp.media.MusicService
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.CommandReceiver
import kotlinx.coroutines.ExperimentalCoroutinesApi


/**
 * Android Automotive specific extensions for [MusicService].
 *
 * UAMP for Android Automotive OS requires the user to login in order to demonstrate
 * how authentication works on the system. If this doesn't apply to your application,
 * this class can be skipped in favor of its parent, [MusicService].
 *
 * If you'd like to support authentication, but not prevent using the system,
 * comment out the calls to [requireLogin].
 */
class AutomotiveMusicService : MusicService() {

}