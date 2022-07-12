package com.starbowproj.musicplayer.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.*
import com.starbowproj.musicplayer.databinding.ActivityMusicBinding
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.room.Playlist
import kotlinx.coroutines.*

class MusicActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMusicBinding.inflate(layoutInflater) }
    private val musicPlayer by lazy { MusicPlayer.getInstance() }

    private var seekBarTouching = false //사크바 값 조정중인지 확인하기 위한 프로퍼티

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        EventBus.getInstance().register(this)

        //marquee 기능을 사용하기 위해 isSelected 값을 true로 설정
        binding.textTitle.isSelected = true
        binding.textArtist.isSelected = true

        //툴바 설정 및 홈버튼 활성화, 기본 타이틀 비활성화
        setSupportActionBar(binding.toolbarMusic)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.imageAlbumArt.clipToOutline = true

        //리스너 세팅
        setListeners()

        //반복, 셔플 재생 여부에 맞춰서 아이콘 변경
        setRepeatButtonIcon()
        setShuffleButtonIcon()

        val curPlaylist = intent.getSerializableExtra("playlist") as Playlist?
        val curOrder = intent.getIntExtra("curPosition", musicPlayer.getCurrentOrder() ?: 0)

        Log.d("현재곡", "Playlist = ${curPlaylist?.name}, CurrentOrder = ${curOrder}")

        setMusic(musicPlayer.getMusic(curOrder))

        //현재 플레이리스트 혹은 재생/일시정지 중인 음원과 설정한 플레이리스트 및 음원이 다르면 음원 새로 재생
        if(musicPlayer.getPlaylistId() != curPlaylist?.no || musicPlayer.getCurrentMusicId() != musicPlayer.getMusic(curOrder).id)
        {
            musicPlayer.setPlaylist(curPlaylist)
            musicPlayer.playMusic(curOrder)
        }
    }

    override fun onDestroy() {
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> { //홈 버튼을 누르면
                onBackPressed() //뒤로가기 작동
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe
    fun doEvent(event: Event) {
        val curMusic = musicPlayer.getCurrentMusic()

        when(event.getEvent()) {
            "PLAY_NEW_MUSIC" -> { //새로운 음원 재생
                if(curMusic != null) {
                    setMusic(curMusic)
                } else {
                    Toast.makeText(this, "현재 재생중인 음원을 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            "PLAY" -> { //기존 음원 재생
                workSeekbar()
                binding.btnPlay.setImageResource(R.drawable.pause)
            }
            "PAUSE" -> { //일시정지
                binding.btnPlay.setImageResource(R.drawable.play_arrow)
            }
            "STOP" -> { //정지
                finish()
            }
        }
    }

    //음원 관련 정보를 액티비티에 세팅
    private fun setMusic(music: Music) {
        //화면 세팅
        binding.run {
            //타이틀, 아티스트, 앨범아트, 길이 표시
            textTitle.text = music.title
            textArtist.text = music.artist
            imageAlbumArt.setImageURI(music.getAlbumUri())
            if (imageAlbumArt.drawable == null) {
                imageAlbumArt.setImageResource(R.drawable.default_album_art)
            }
            //시크바 설정
            val musicDuration = music.duration ?: 0
            val curPos = musicPlayer.getCurrentPosition()
            seekBar.max = musicDuration
            textDuration.text = getString(R.string.duration, musicDuration/60000, (musicDuration/1000)%60)
            binding.seekBar.progress = curPos
            textCurrent.text = getString(R.string.duration, curPos/60000, (curPos/1000)%60)
            //버튼 아이콘 설정
            if (musicPlayer.isPlaying()) {
                binding.btnPlay.setImageResource(R.drawable.pause)
                workSeekbar()
            }
            else
                btnPlay.setImageResource(R.drawable.play_arrow)
        }
    }

    //반복 모드 버튼 아이콘 설정
    private fun setRepeatButtonIcon() {
        val repeatMode = musicPlayer.getRepeatMode()

        when(repeatMode) {
            Repeat.REPEAT_OFF -> binding.btnRepeat.setImageResource(R.drawable.repeat)
            Repeat.REPEAT -> binding.btnRepeat.setImageResource(R.drawable.repeat_on)
            Repeat.REPEAT_ONE -> binding.btnRepeat.setImageResource(R.drawable.repeat_one_on)
        }
        Log.d("모드", "repeat_mode = ${repeatMode}")
    }

    //셔플 버튼 아이콘 설정
    private fun setShuffleButtonIcon() {
        val shuffle = musicPlayer.getShuffleMode()

        when(shuffle) {
            false -> binding.btnShuffle.setImageResource(R.drawable.shuffle)
            true -> binding.btnShuffle.setImageResource(R.drawable.shuffle_on)
        }
        Log.d("모드", "shuffle = ${shuffle}")
    }

    //버튼 5개 각각에 클릭리스너 세팅
    private fun setListeners() {
        binding.run {
            btnPlay.setOnClickListener { // 재생/일시정지 버튼
                if(musicPlayer.isPlaying()) musicPlayer.pause() else { musicPlayer.start() }
            }
            btnPrev.setOnClickListener { //이전 음원 재생 버튼
                musicPlayer.skipPrev()
            }
            btnNext.setOnClickListener { //다음 음원 재생 버튼
                musicPlayer.skipNext()
            }
            btnRepeat.setOnClickListener { //반복재생 버튼 (반복 X -> 전체 반복 -> 한곡 반복 순으로 순환)
                musicPlayer.changeRepeatMode()
                setRepeatButtonIcon()
            }
            btnShuffle.setOnClickListener { //셔플 재생 on/off
                musicPlayer.changeShuffleMode()
                setShuffleButtonIcon()
            }
            seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener { //시크바 값 변경 시
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { //시크바 값이 변하면
                    if(fromUser) { //사용자가 조절하는 경우
                        seekBarTouching = true //터치중인 경우는 weekBarTouching을 true로 유지
                        binding.textCurrent.text = getString(R.string.duration, progress/60000000, (progress/1000000)%60)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {  }

                override fun onStopTrackingTouch(seekBar: SeekBar?) { //터치가 종료되면
                    seekBarTouching = false //터지가 종료됏으니 false로 전환
                    workSeekbar() //다시 음원에 맞춰서 시크바 작동시킴

                    try {
                        musicPlayer.seekTo(seekBar!!.progress) //변경된 시크바 값을 토대로 음원 재생 위치 설정
                    } catch(e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MusicActivity, "문제가 발생했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    //MediaPlayer의 getCurrentPosition()을 통해서 현재 재생 위치를 받아 시크바에 반영 (0.01초 단위로)
    fun workSeekbar() {
        CoroutineScope(Dispatchers.Main).launch {
            while(musicPlayer.isPlaying() && !seekBarTouching) {
                val curPos = musicPlayer.getCurrentPosition()
                binding.seekBar.progress = curPos
                binding.textCurrent.text = getString(R.string.duration, curPos/60000, (curPos/1000)%60)
                delay(10)
            }
        }
    }
}