package com.starbowproj.musicplayer.activity

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.*
import com.starbowproj.musicplayer.databinding.ActivityMainBinding
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.fragment.ListFragment
import com.starbowproj.musicplayer.service.MusicService

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val musicPlayer by lazy { MusicPlayer.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)
        binding.textToolbarIcon.setImageResource(R.drawable.musical_notes)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        EventBus.getInstance().register(this)

        musicPlayer.initPlayer(this) //뮤직 플레이어 초기화

        setFragment()
        setMiniPlayerListener()

        binding.textMiniTitle.isSelected = true
        binding.textMiniArtist.isSelected = true
        binding.imageMiniAlbumView.clipToOutline = true
    }

    override fun onStart() {
        super.onStart()
        setMiniPlayer(musicPlayer.getCurrentMusic())
    }

    override fun onDestroy() {
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun doEvent(event: Event) {
        val curMusic = musicPlayer.getCurrentMusic()

        when(event.getEvent()) {
            "PLAY_NEW_MUSIC" -> { //새로운 음원이 재생
                showMiniPlayer()
                setMiniPlayer(curMusic)
                musicServiceStart()
            }
            "PLAY", "PAUSE" -> { //재생(기존 음원), 일시정지
                setPlayPauseButtonIcon()
                musicServiceStart()
            }
            "STOP" -> { //정지
                hideMiniPlayer()
            }
            "PERMISSION_DENIED" -> { //권한 거부
                finish()
            }
            "OPEN_PLAYLIST_DETAILED" -> { //플레이리스트의 상세 곡 정보가 열릴 때
                //플레이리스트 정보를 띄울 텍스트 뷰들을 활성화
                binding.textPlaylistTitle.visibility = View.VISIBLE
                binding.textNumMusic.visibility = View.VISIBLE
                //타이틀과 아이콘은 숨김
                binding.textToolbarIcon.visibility = View.GONE
                binding.textToolbarTitle.visibility = View.GONE
                //
                binding.textPlaylistTitle.text = event.getData() as String
            }
            "CLOSE_PLAYLIST_DETAILED" -> { //플레이리스트의 상세 곡 정보가 닫힐 때
                //플레이리스트 정보를 띄울 텍스트 뷰들을 숨김
                binding.textPlaylistTitle.visibility = View.GONE
                binding.textNumMusic.visibility = View.GONE
                binding.textSorting.visibility = View.GONE
                //타이틀과 아이콘은 활성화
                binding.textToolbarIcon.visibility = View.VISIBLE
                binding.textToolbarTitle.visibility = View.VISIBLE
            }
            "CHANGE_PLAYLIST_NUM_MUSIC" -> { //플레이리스트의 곡 수 가 변경될 때
                binding.textNumMusic.text = getString(R.string.playlist_num_music, event.getData() as Int)
            }
            "START_SORTING" -> { //플레이리스트 곡 목록의 정렬 작업이 시작될 때
                binding.textPlaylistTitle.visibility = View.GONE
                binding.textNumMusic.visibility = View.GONE
                binding.textSorting.visibility = View.VISIBLE
            }
            "FINISH_SORTING" -> { //플레이리스트 곡 목록의 정렬 작업이 종료될 때
                binding.textPlaylistTitle.visibility = View.VISIBLE
                binding.textNumMusic.visibility = View.VISIBLE
                binding.textSorting.visibility = View.GONE
            }
        }
    }

    //미니 플레이어 관련 메서드
    //------------------------------------------------------------------------------------
    //미니 플레이어 세팅
    private fun setMiniPlayer(music: Music?) {
        music?.let {
            //화면 세팅
            binding.run {
                //타이틀, 아티스트, 앨범아트, 길이 표시
                textMiniTitle.text = it.title
                textMiniArtist.text = it.artist
                imageMiniAlbumView.setImageURI(it.getAlbumUri())
                if (imageMiniAlbumView.drawable == null) {
                    imageMiniAlbumView.setImageResource(R.drawable.default_album_art)
                }

                if(musicPlayer.isPlaying())
                    btnMiniPlay.setImageResource(R.drawable.pause)
                else
                    btnMiniPlay.setImageResource(R.drawable.play_arrow)
            }
        }
    }

    //미니 플레이어에서 사용하는 버튼에 리스너 세팅
    private fun setMiniPlayerListener() {
        binding.run {
            //미니 플레이어 레이아웃을 클릭 시
            miniPlayer.setOnClickListener {
                val intent = Intent(this@MainActivity, MusicActivity::class.java)
                intent.putExtra("playlist", musicPlayer.getPlaylist())
                intent.putExtra("curPosition", musicPlayer.getCurrentOrder())
                startActivity(intent)
            }
            //재생/일시정지
            btnMiniPlay.setOnClickListener {
                if(musicPlayer.isPlaying()) musicPlayer.pause() else musicPlayer.start()
            }
            //이전 곡
            btnMiniPrev.setOnClickListener {
                musicPlayer.skipPrev()
            }
            //다음 곡
            btnMiniNext.setOnClickListener {
                musicPlayer.skipNext()
            }
        }
    }

    //미니 플레이어 보여주기
    private fun showMiniPlayer() { binding.miniPlayer.visibility = View.VISIBLE }

    //미니 플레이어 가리기
    private fun hideMiniPlayer() { binding.miniPlayer.visibility = View.GONE }

    //재생/일시정지 버튼의 아이콘을 플레이어의 상태에 맞춰서 변경
    private fun setPlayPauseButtonIcon() {
        if(musicPlayer.isPlaying()) {
            binding.btnMiniPlay.setImageResource(R.drawable.pause)
        } else {
            binding.btnMiniPlay.setImageResource(R.drawable.play_arrow)
        }
    }
    //------------------------------------------------------------------------------------

    //액티비티에 ListFragment 세팅
    private fun setFragment() {
        val listFragment = ListFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.frameLayout, listFragment)
        transaction.commit()
    }

    //서비스를 시작하는 메서드
    private fun musicServiceStart() {
        val serviceIntent = Intent(this, MusicService::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}