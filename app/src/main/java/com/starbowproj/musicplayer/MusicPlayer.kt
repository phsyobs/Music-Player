package com.starbowproj.musicplayer

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.room.PlayCountRoomHelper
import com.starbowproj.musicplayer.room.Playlist
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

enum class Repeat {
    REPEAT_OFF, REPEAT, REPEAT_ONE
}

//MusicActivity같은 액티비티들이 호출될 때마다 MediaPlayer 객체가 새로 생성되는 것을 막기위해 작성한 클래스 (싱글톤 패턴 참고)
class MusicPlayer private constructor() {
    companion object {
        private var musicPlayer: MusicPlayer? = null

        fun getInstance(): MusicPlayer = musicPlayer
            ?: MusicPlayer().apply { musicPlayer = this }
    }

    enum class State {
        PLAY, PAUSE, STOP
    }

    private var mediaPlayer: MediaPlayer? = null //미디어 플레이어
    private var mContext: Context? = null

    //------------------------------------------------------------------------------------
    private var currentPlaylist: Playlist? = null //현재 사용중인 플레이리스트
    private var musicList: ArrayList<Music> = arrayListOf<Music>() //재생할 음원 리스트
    private var currentMusic: Music? = null //현재 재생중인 음원
    private var curOrder: Int = -1 //음원 리스트 기준 현재 재생되는 음원의 order

    private var state: State = State.STOP //현재 플레이어 상태
    private var repeatMode = Repeat.REPEAT_OFF //반복 모드 (반복 X, 전체 반복, 한곡만 반복)
    private var isShuffle = false //셔플 재생 활성화 여부
    private var shuffleStack = Stack<Int>() //셔플 재생시 이전에 재생된 곡의 인덱스를 저장

    private var totalPlaytime = 0 //총 재생 시간(초 단위, mod 100hours)
    private var numStar = 0 //총 재생시간을 100시간으로 나눈 몫
    //------------------------------------------------------------------------------------

    private var adapterList = mutableListOf<RecyclerView.Adapter<*>>() //음원과 플레이리스트 목록을 띄우는데 사용하는 어댑터들의 리스트

    private lateinit var sharedPlaymode: SharedPreferences //재생 모드(반복, 랜덤)를 저장하는 SharedPreferences
    private lateinit var editorPlaymode: SharedPreferences.Editor

    private var workPlaytimeJob: Job? = null

    //플레이어 초기화 관련
    //------------------------------------------------------------------------------------
    fun initPlayer(context: Context) { //플레이어에서 사용할 기본적인 값들을 설정해준다
        mContext = context //사용할 컨텍스트

        setCompleteListener()

        setSharedPreferences() //SharedPreferences 설정
        initPlaymode() //저장된 반복/셔플 재생 모드 받아오기
        initTotalPlayTime() //저장된 총 재생시간 받아오기
    }

    private fun setSharedPreferences() {
        sharedPlaymode = mContext!!.getSharedPreferences("play_mode", Context.MODE_PRIVATE)
        editorPlaymode = sharedPlaymode.edit()
    }

    private fun initPlaymode() { //최근에 설정한 반복 및 랜덤 재생 모드로 설정
        repeatMode = when(sharedPlaymode.getInt("repeatMode", 0)) {
            0 -> Repeat.REPEAT_OFF
            1 -> Repeat.REPEAT
            2 -> Repeat.REPEAT_ONE
            else -> {
                Toast.makeText(mContext, "값을 설정하는데 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
                Repeat.REPEAT_OFF
            }
        }
        isShuffle = sharedPlaymode.getBoolean("shuffle", false)
    }

    private fun initTotalPlayTime() {
        val sharedPreferences = mContext!!.getSharedPreferences("statistics", Context.MODE_PRIVATE)
        totalPlaytime = sharedPreferences.getInt("totalPlaytime", 0)
    }

    private fun setCompleteListener() {
        mediaPlayer?.setOnCompletionListener {
            curOrder = getCurrentOrder() ?: -1 //중간에 리스트가 변했을수도 있으므로 order를 다시 받음
            //반복 재생이 꺼져있고 재생 완료한 음원이 마지막 음원일 때
            if (repeatMode == Repeat.REPEAT_OFF && curOrder == musicList.size - 1) {
                //리스트 맨 앞으로 이동한 후 일시정지 상태로 설정한다.
                //마지막 음원에서 그대로 멈추는 것보다 첫번째 음원으로 전환후 멈추는것이 사용자 입장에서는 편하게 첫 음원부터 재생할 수 있어 더 좋을것이라고 판단
                playMusic(0)
                pause()
            } else {
                curOrder = nextPosition(curOrder)
                playMusic(curOrder)
            }
        }
    }
    //------------------------------------------------------------------------------------

    //총 재생시간 관련 메서드
    //------------------------------------------------------------------------------------
    private fun workPlaytime() { //총 재생시간 타이머 작동
        workPlaytimeJob = CoroutineScope(Dispatchers.Main).launch {
            while(mediaPlayer?.isPlaying == true) {
                delay(1000)
                totalPlaytime++
                if(totalPlaytime >= 360000) {
                    numStar++
                    totalPlaytime -= 360000
                }
                Log.d("totalTime", "star = ${numStar}, time = ${totalPlaytime}sec")
                saveStatistics()
                EventBus.getInstance().post(Event("INCREASE_TOTAL_PLAYTIME", totalPlaytime))
            }
        }
    }

    private fun stopPlaytime() { //총 재생시간 타이머 작동 중지
        if (workPlaytimeJob != null) {
            workPlaytimeJob?.cancel()
            workPlaytimeJob = null
        }
    }

    private fun saveStatistics() { //SharedPreferences에 통계정보 저장 (타이머 지연을 약간이나마 방지하기 위해 코루틴에서 실행)
        CoroutineScope(Dispatchers.IO).launch {
            val sharedPreferences =
                mContext!!.getSharedPreferences("statistics", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("totalPlaytime", totalPlaytime)
            editor.apply()
        }
    }
    //------------------------------------------------------------------------------------

    //플레이어 조작 관련
    //------------------------------------------------------------------------------------
    fun playMusic(position: Int) { //플레이어에 음원을 세팅하고 재생 (mediaPlayer가 null일때 작동)
        val music = musicList[position]

        //음원이 중복해서 들리는걸 방지하기 위해 해제 후 새 음원으로 세팅
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
            stopPlaytime()
            currentMusic = null
            curOrder = -1
        }

        mediaPlayer = MediaPlayer.create(mContext, music.getMusicUri())
        setCompleteListener()
        mediaPlayer?.start()
        if (state == State.PAUSE) {
            mediaPlayer?.seekTo(getCurrentPosition())
            Log.d("MusicPlayer", "MusicPosition = ${getCurrentPosition()/1000000}/${(music.duration ?: 0)/1000000}")
        }

        state = State.PLAY

        workPlaytime()
        currentMusic = music
        curOrder = position
        Log.d("MusicPlayer", "Playlist = ${currentPlaylist?.name}")
        Log.d("MusicPlayer", "changeMusic = ${music.title}")
        Log.d("현재곡", "!!!! Playlist = ${currentPlaylist?.name}, CurrentOrder = ${curOrder}")

        PlayCountRoomHelper.getHelper(mContext!!).playCountDao().countUp(music.id)
        EventBus.getInstance().post(Event("CHANGE_PLAYCOUNT"))

        EventBus.getInstance().post(Event("PLAY_NEW_MUSIC"))

        Log.d("MusicPlayer", "num = ${musicList.size}, current = ${position+1}/${musicList.size}")
    }

    fun start() { //재생 (일시정지 상태에서 재생)
        state = State.PLAY
        mediaPlayer?.start()
        Log.d("MusicPlayer", "State = PLAY")
        workPlaytime()

        EventBus.getInstance().post(Event("PLAY"))
    }

    fun pause() { //일시정지
        state = State.PAUSE
        Log.d("MusicPlayer", "State = PAUSE")
        mediaPlayer?.pause()

        EventBus.getInstance().post(Event("PAUSE"))
    }

    fun stop() { //정지 (플레이어 해제)
        state = State.STOP
        Log.d("MusicPlayer", "State = STOP")
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusic = null
        currentPlaylist = null

        EventBus.getInstance().post(Event("STOP"))
    }

    fun skipPrev() { //이전 곡
        if(isShuffle) { //셔플 재생 중이면
            shuffleStack.pop()
            if(shuffleStack.isNotEmpty()) { //shuffleStack에 요소가 있으면
                curOrder = shuffleStack.peek() //스택 제일 위에 있는 값을 받음
            } else { //요소가 없으면 랜덤 재생
                curOrder = (0 until musicList.size).random()
                shuffleStack.push(curOrder)
            }
        } else {
            curOrder = if(curOrder == 0) musicList.size -1 else curOrder-1
        }

        playMusic(curOrder)
    }

    fun skipNext() { //다음 곡
        curOrder = if(isShuffle) { //셔플 재생이 켜져있으면 랜덤으로 다음 곡 재생
            val next = (0 until musicList.size).random()
            shuffleStack.push(next)
            next
        } else {
            if (curOrder == musicList.size-1) 0 else curOrder + 1
        }

        playMusic(curOrder)
    }

    fun seekTo(target: Int) { //음원 재생위치 변경
        mediaPlayer?.seekTo(target)
    }
    //------------------------------------------------------------------------------------

    //반복 모드, 셔플 재생 관련 메서드
    //------------------------------------------------------------------------------------
    fun changeRepeatMode() {
        repeatMode = when(repeatMode) {
            Repeat.REPEAT_OFF -> Repeat.REPEAT
            Repeat.REPEAT -> Repeat.REPEAT_ONE
            Repeat.REPEAT_ONE -> Repeat.REPEAT_OFF
        }

        editorPlaymode.putInt("repeatMode", repeatMode.ordinal)
        editorPlaymode.apply()
    }

    fun changeShuffleMode() {
        if(isShuffle) {
            isShuffle = false
            shuffleStack.clear()
        } else {
            isShuffle = true
            shuffleStack.add(curOrder)
        }

        editorPlaymode.putBoolean("shuffle", isShuffle)
        editorPlaymode.apply()
    }

    fun getRepeatMode(): Repeat {
        return repeatMode
    }

    fun getShuffleMode(): Boolean {
        return isShuffle
    }
    //------------------------------------------------------------------------------------

    //플레이어의 상태를 확인
    //------------------------------------------------------------------------------------
    fun isPlaying() = (state == State.PLAY)
    fun isPause() = (state == State.PAUSE)
    fun isStop() = (state == State.STOP)
    //------------------------------------------------------------------------------------

    //플레이어에 설정된 특정 값을 반환하는 메서드들
    //------------------------------------------------------------------------------------
    fun getMusic(index: Int): Music { //해당 인덱스에 들어있는 Music 객체 반환
        return musicList[index]
    }

    //현재 재생중인 음원을 얻어오는 메서드
    fun getCurrentMusic(): Music? = currentMusic

    //현재 재생중인 음원의 id를 얻어오는 메서드
    fun getCurrentMusicId(): String? = currentMusic?.id

    fun getCurrentOrder(): Int? { //현재 재생중인 음원의 order를 얻어오는 메서드
        for(i in 0 until musicList.size) {
            if(musicList[i].id == currentMusic?.id)
                return i
        }
        return null
    }
    //기존에 musicList.indexOf(currentMusic)로만 작성했으나 버그가 발생
    //원인은 SelectedPlaylistActivity에서 onStart()를 거치면서 리스트를 새로 생성해 화면을 띄우기 때문
    //리스트를 새로 만들면서 객체들의 값은 동일하나 참조값이 바껴 index를 찾지 못하고 -1을 뱉어버림

    fun getPlaylist(): Playlist? = currentPlaylist

    //현재 재생중인 플레이리스트의 id
    fun getPlaylistId(): Long? { return currentPlaylist?.no }

    //현재 음원 재생 위치
    fun getCurrentPosition() = mediaPlayer?.currentPosition ?: -1

    //총 재생 시간
    fun getTotalPlayTime() = totalPlaytime
    fun getStar() = numStar
    //------------------------------------------------------------------------------------

    //플레이어에 플레이리스트와 음원 리스트를 설정하고 조작하는 메서드들
    //------------------------------------------------------------------------------------
    fun setMusicList(musicList: List<Music>) { //음원 리스트를 세팅
        this.musicList.clear()
        this.musicList.addAll(musicList)
    }

    fun deleteMusic(music: Music) { //음원 하나 삭제
        musicList.remove(music)
    }

    //선택된 플레이리스트를 세팅
    fun setPlaylist(playlist: Playlist?) { currentPlaylist = playlist }
    //------------------------------------------------------------------------------------

    //다음 재생할 음원의 order 반환
    private fun nextPosition(position: Int): Int {
        val nextPosition: Int

        when(repeatMode) {
            Repeat.REPEAT_OFF -> {
                nextPosition = if (isShuffle) {
                    val next = (0 until musicList.size).random()
                    shuffleStack.add(next)
                    next
                } else {
                    position + 1
                }
            }
            Repeat.REPEAT -> {
                nextPosition = if (isShuffle) {
                    val next = (0 until musicList.size).random()
                    shuffleStack.add(next)
                    next
                } else {
                    if (position == musicList.size-1) 0 else position + 1
                }
            }
            Repeat.REPEAT_ONE -> nextPosition = position
        }

        return nextPosition
    }

    //플레이리스트나 음원 목록을 띄우는데 사용하는 리사이클러뷰의 어댑터들을 adapterList에 추가하기 위해 사용하는 메서드
    fun addAdapter(adapter: RecyclerView.Adapter<*>?) {
        if(adapter == null || adapterList.contains(adapter))
            return

        adapterList.add(adapter)
    }
}