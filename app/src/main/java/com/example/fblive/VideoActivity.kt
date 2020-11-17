package com.example.fblive

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Insets
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_video.*
import java.io.Serializable


class VideoActivity : AppCompatActivity(), VideoFragmentListener {
    private var videoInterface: VideoFragmentInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }

        if (!playVideo(extras)) {
            finish()
            return
        }
    }

    // VideoFragmentListener
    override fun videoEnded() {
        finish()
    }

    private fun playVideo(extras: Bundle): Boolean {
        val width = extras.getString(EXTRA_SCALE_WIDTH)?.toIntOrNull()
        val height = extras.getString(EXTRA_SCALE_HEIGHT)?.toIntOrNull()
        if (width != null && height != null) {
            val windowHeight =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val windowInsets = windowManager.currentWindowMetrics.windowInsets
                    val insets: Insets = windowInsets.getInsetsIgnoringVisibility(
                        WindowInsets.Type.navigationBars()
                                or WindowInsets.Type.displayCutout()
                    )
                    insets.top + insets.bottom
                } else {
                    val displayMetrics = DisplayMetrics()
                    windowManager.defaultDisplay.getMetrics(displayMetrics)
                    displayMetrics.heightPixels
                }

            rootView.layoutParams.width =
                (width.toFloat() / height * windowHeight).toInt()
        }

        // Set main video fragment
        val fragment = replaceFragment(R.id.fragmentContainer, extras, null)
        if (fragment is VideoFragmentInterface) {
            videoInterface = fragment
        }

        // Set other fragment
        viewLoop@ for (index in 0..rootView.childCount) {
            val view = rootView.getChildAt(index)
            val tag = view?.tag as String?
            if (tag.isNullOrBlank()) {
                continue
            }
            replaceFragment(view.id, extras, tag)
        }
        return true
    }

    private fun replaceFragment(viewId: Int, extras: Bundle, tag: String? = null): Fragment? {
        val prefix: String = tag?.let { "$it-" } ?: ""
        val urls = extras.getString("$prefix$EXTRA_URLS") ?: return null
        val option = Option(extras.getString("$prefix$EXTRA_IS_REPEAT")?.toBoolean() ?: false, true)
        return when (extras.getString("$prefix$EXTRA_TYPE")) {
            "facebook-video" -> {
                VideoFbFragment.newInstance(urls, option)
            }
            else -> VideoFbFragment.newInstance(urls, option)
        }.also { fragment ->
            supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.let {
                supportFragmentManager
                    .beginTransaction()
                    .remove(it)
                    .commit()
            }
            supportFragmentManager
                .beginTransaction()
                .replace(viewId, fragment)
                .commit()
        }
    }

    private fun pauseVideo() {
        videoInterface?.pauseVideo()
    }

    private fun resumeVideo() {
        videoInterface?.resumeVideo()
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_URLS = "urls"
        const val EXTRA_TAG = "tag"
        const val EXTRA_IS_REPEAT = "repeatValue"

        const val EXTRA_SCALE_WIDTH = "displayWidth"
        const val EXTRA_SCALE_HEIGHT = "displayHeight"
    }

    class Option(
        val isRepeat: Boolean = false,
        val isScaleCropInside: Boolean = true
    ) : Serializable
}