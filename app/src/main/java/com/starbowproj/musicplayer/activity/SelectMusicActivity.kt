package com.starbowproj.musicplayer.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import com.starbowproj.musicplayer.Music
import com.starbowproj.musicplayer.adapter.MusicAdapter
import com.starbowproj.musicplayer.databinding.ActivitySelectMusicBinding
import com.starbowproj.musicplayer.room.Playlist
import com.starbowproj.musicplayer.room.PlaylistMusic
import com.starbowproj.musicplayer.room.PlaylistRoomHelper

class SelectMusicActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySelectMusicBinding.inflate(layoutInflater) }
    private lateinit var helper: PlaylistRoomHelper

    //생성될 플레이리스트의 no, name
    private var playlistNo = 0L
    private lateinit var playlistName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        playlistNo = intent.getLongExtra("no", 0)
        playlistName = intent.getStringExtra("name") ?: ""

        //SQLite를 사용하기 위한 Room 헬퍼 생성
        helper = PlaylistRoomHelper.getHelper(this)

        val adapter = MusicAdapter(this)
        adapter.musicList.addAll(getMusicList())
        val checkList = mutableListOf<Boolean>()
        for (i in 0 until adapter.musicList.size) {
            checkList.add(false)
        }
        adapter.checkList = checkList
        adapter.useCheckbox = true
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.btnAddinPlaylist.setOnClickListener {
            val playlist = Playlist(playlistNo, playlistName) //Playlist 객체 생성

            helper.playlistDao().insertPlaylist(playlist) //데이터베이스에 플레이리스트를 추가 (IGNORE에 의해 이미 존재하는 경우 새로 추가되지 않음)

            val beforeNumMusic = helper.playlistMusicDao().getNumMusic(playlist.no)

            for (i in 0 until adapter.checkList.size) {
                if (adapter.checkList[i]) { //체크박스에 체크가 되어있는 음원은
                    val music = adapter.musicList[i]
                    val playlistMusic = PlaylistMusic(
                        music.id,
                        playlist.no,
                        beforeNumMusic + i,
                        music.title,
                        music.artist,
                        music.albumId,
                        music.duration
                    )

                    helper.playlistMusicDao().insertMusic(playlistMusic) //데이터베이스에 추가 (IGNORE에 의해 이미 존재하는 경우 새로 추가되지 않음)
                }
            }
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    //MusicListFragment에서 구현한것과 동일하다.
    //즉, MusicListFragment에서 얻은 음원 리스트를 아래와 같은 과정 없이 그대로 재활용 해서 사용하는 방법을 생각해볼 필요가 있음!!
    private fun getMusicList(): List<Music> {
        val musicList = mutableListOf<Music>()

        val listUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )

        //프래그먼트에서 콘텐트리졸버를 사용하기 위해서는 getContext()(또는 context)를 먼저 호출해야 한다.
        val cursor = contentResolver?.query(listUri, proj, null, null, null)

        if(cursor != null) {
            while (cursor.moveToNext()) {
                var index = cursor.getColumnIndex(proj[0])
                val id = cursor.getString(index)

                index = cursor.getColumnIndex(proj[1])
                val title = cursor.getString(index)

                index = cursor.getColumnIndex(proj[2])
                val artist = cursor.getString(index)

                index = cursor.getColumnIndex(proj[3])
                val albumId = cursor.getString(index)

                index = cursor.getColumnIndex(proj[4])
                val duration = cursor.getInt(index)

                musicList.add(Music(id, title, artist, albumId, duration))
            }
        }

        cursor?.close() //Cursor는 사용이 끝나면 해제해줘야 한다. 잊지 말 것!

        return musicList
    }
}