package com.xplayer.media

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xplayer.media.ui.components.EmptyLibraryState
import com.xplayer.media.ui.components.PermissionGateCard
import com.xplayer.media.ui.theme.XPlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XPlayerUiAndPlaybackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionGateCard_rendersAndTriggersCallbacks() {
        var permissionRequested = false
        var singleFileOpened = false

        composeTestRule.setContent {
            XPlayerTheme {
                PermissionGateCard(
                    onRequestPermission = { permissionRequested = true },
                    onOpenSingleFile = { singleFileOpened = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("PermissionGate").assertIsDisplayed()
        composeTestRule.onNodeWithTag("GrantPermissionButton").performClick()
        composeTestRule.onNodeWithTag("OpenSingleFileButton").performClick()

        assertEquals(true, permissionRequested)
        assertEquals(true, singleFileOpened)
    }

    @Test
    fun emptyLibraryState_rendersSafelyWithoutCrashing() {
        composeTestRule.setContent {
            XPlayerTheme {
                EmptyLibraryState(
                    onRefresh = {},
                    onOpenExternalFile = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("EmptyLibraryState").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Media Found").assertIsDisplayed()
    }

    @Test
    fun exoPlayer_handlesUnsupportedOrCorruptedFileWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val player = ExoPlayer.Builder(context).build()
            try {
                val invalidItem = MediaItem.fromUri(Uri.parse("file:///non_existent_corrupted_file.xyz"))
                player.setMediaItem(invalidItem)
                player.prepare()
                assertEquals(Player.REPEAT_MODE_OFF, player.repeatMode)
            } finally {
                player.release()
            }
        }
    }
}
