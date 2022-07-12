package com.starbowproj.musicplayer

import android.net.Uri
import android.provider.MediaStore
import java.io.Serializable

//인텐트에 Music 객체를 넘겨주기 위해 Serializable 인터페이스를 implement함
class Music(var id: String, var title: String?, var artist: String?, var albumId: String?, var duration: Int?): Serializable {
    fun getMusicUri(): Uri {
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

    fun getAlbumUri(): Uri {
        return Uri.parse("content://media/external/audio/albumart/" + albumId)
    }
}

