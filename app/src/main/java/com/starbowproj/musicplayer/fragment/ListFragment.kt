package com.starbowproj.musicplayer.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.starbowproj.musicplayer.R
import com.starbowproj.musicplayer.adapter.FragmentAdapter
import com.starbowproj.musicplayer.activity.MainActivity
import com.starbowproj.musicplayer.databinding.FragmentListBinding

class ListFragment : Fragment() {
    private lateinit var binding: FragmentListBinding
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListBinding.inflate(inflater, container, false)

        //뷰페이져에 넣을 프래그먼트 리스트
        val fragmentList = listOf(MusicListFragment(), PlaylistFragment(), StatisticsFragment())

        //어댑터 생성 및 뷰페이저에 연결
        val adapter = FragmentAdapter(mContext as MainActivity)
        adapter.fragmentList = fragmentList
        binding.viewPager.adapter = adapter

        //탭 매뉴로 사용할 리스트 생성 후 탭과 뷰페이저 연결
        val tabTitles = listOf<String>("곡", "플레이리스트", "통계")
        val tabIcons = listOf(R.drawable.music_note_24, R.drawable.queue_music_24, R.drawable.bar_chart_24)
        TabLayoutMediator(binding.tabMain, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
            tab.setIcon(tabIcons[position])
        }.attach()

        return binding.root
    }
}