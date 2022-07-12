package com.starbowproj.musicplayer.fragment

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.Music
import com.starbowproj.musicplayer.adapter.MusicAdapter
import com.starbowproj.musicplayer.MusicPlayer
import com.starbowproj.musicplayer.databinding.FragmentMusicListBinding
import com.starbowproj.musicplayer.room.PlayCount
import com.starbowproj.musicplayer.room.PlayCountRoomHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicListFragment : Fragment() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }

    private lateinit var binding: FragmentMusicListBinding
    private lateinit var mContext: Context
    private val adapter by lazy { MusicAdapter(mContext) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        EventBus.getInstance().register(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if(isGranted) {
                startProcess()
            } else {
                Toast.makeText(mContext, "외부 저장소 권한을 승인해야 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
                EventBus.getInstance().post(Event("PERMISSION_DENIED"))
            }
        } //권한 요청을 위한 런처

        storagePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE) //외부 저장소 권한 요청
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMusicListBinding.inflate(inflater, container, false)
        binding.musicRecyclerView.setEmptyView(binding.textEmptyMusic)
        return binding.root
    }

    override fun onDestroy() {
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun doEvent(event: Event) {
        when(event.getEvent()) {
            "PLAY_NEW_MUSIC", "STOP" -> {
                adapter.notifyItemRangeChanged(0, adapter.musicList.size)
            }
        }
    }

    private fun startProcess() {
        //리사이클러뷰 어댑터 생성 및 연결
        musicPlayer.addAdapter(adapter)
        adapter.useCheckbox = false
        adapter.musicList.addAll(getMusicList())
        binding.musicRecyclerView.adapter = adapter
        binding.musicRecyclerView.layoutManager = LinearLayoutManager(context)
    }

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
        val cursor = context?.contentResolver?.query(listUri, proj, null, null, null)

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

                //재생 횟수를 카운트 하기 위해 데이터베이스에 곡 정보와 재생횟수를 0으로 해서 추가 시킨다.
                //이미 데이터가 있는 음원의 경우 새로 insert 되지 않고 무시된다.
                CoroutineScope(Dispatchers.IO).launch {
                    PlayCountRoomHelper.getHelper(mContext)
                        .playCountDao()
                        .insertPlayCount(PlayCount(id, title, artist, albumId, duration, 0))
                }
            }
        }

        cursor?.close() //커서 사용 끝나면 꼭 닫을것!

        return musicList
    }
}