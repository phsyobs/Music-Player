package com.starbowproj.musicplayer.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.MusicPlayer
import com.starbowproj.musicplayer.R
import com.starbowproj.musicplayer.adapter.PlayCountAdapter
import com.starbowproj.musicplayer.databinding.FragmentStatisticsBinding
import com.starbowproj.musicplayer.room.PlayCountRoomHelper

class StatisticsFragment : Fragment() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }

    private lateinit var binding: FragmentStatisticsBinding
    private lateinit var mContext: Context
    private var adapter: PlayCountAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        EventBus.getInstance().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        binding.recyclerView.setEmptyView(binding.textEmptyRanking)
        setTimer(musicPlayer.getTotalPlayTime(), musicPlayer.getStar())
        setPlayCountRanking()
        return binding.root
    }

    override fun onDestroy() {
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun doEvent(event: Event) {
        when(event.getEvent()) {
            "INCREASE_TOTAL_PLAYTIME" -> { //총 재생시간 오름
                if(event.getData() is Int) {
                    setTimer(event.getData() as Int, musicPlayer.getStar())
                }
            }
            "CHANGE_PLAYCOUNT", "STOP" -> { //플레이카운트 정보가 변경 됨
                setPlayCountRanking()
            }
        }
    }

    //총 재생시간 타이머 세팅
    private fun setTimer(time: Int, star: Int) {
        val hour = time/3600
        val min = (time%3600)/60
        val sec = time%60
        binding.totalPlaytime.text = mContext.getString(R.string.playtime, hour, min, sec)
        binding.progressBar.progress = time

        if(star > 0) {
            binding.textStar.text = "${star}"
            binding.textStar.visibility = View.VISIBLE
        } else {
            binding.textStar.visibility = View.GONE
        }
    }

    //재생 횟수 순위를 리사이클러뷰를 통해 보여주는 메서드
    private fun setPlayCountRanking() {
        var playCountList = PlayCountRoomHelper.getHelper(mContext).playCountDao().getAllPlayCount()
        playCountList = if(playCountList.size > 100) {
            val playCountSubList = playCountList.subList(0, 100)
            playCountSubList
        } else { playCountList }

        if (adapter != null) {
            adapter!!.playCountList = playCountList
            adapter!!.notifyItemRangeChanged(0, playCountList.size)
        } else {
            adapter = PlayCountAdapter(mContext)
            //순위 목록은 상위 100개만 띄운다
            adapter!!.playCountList = playCountList
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(mContext)

        }
    }
}