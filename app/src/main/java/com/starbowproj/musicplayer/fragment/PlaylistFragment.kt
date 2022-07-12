package com.starbowproj.musicplayer.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.MusicPlayer
import com.starbowproj.musicplayer.R
import com.starbowproj.musicplayer.adapter.PlaylistAdapter
import com.starbowproj.musicplayer.activity.SelectMusicActivity
import com.starbowproj.musicplayer.databinding.FragmentPlaylistBinding
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.room.Playlist
import com.starbowproj.musicplayer.room.PlaylistRoomHelper

class PlaylistFragment : Fragment() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }

    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var mContext: Context
    private lateinit var helper: PlaylistRoomHelper
    private var adapter: PlaylistAdapter? = null

    private lateinit var playlistList: MutableList<Playlist>

    private lateinit var menuHost: MenuHost
    private lateinit var menuProvider: MenuProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        EventBus.getInstance().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        binding.recyclerView.setEmptyView(binding.textEmptyPlaylist)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //SQLite를 사용하기 위한 Room 헬퍼 생성
        helper = PlaylistRoomHelper.getHelper(mContext)

        //툴바에 메뉴버튼을 생성하기 위한 MenuHost
        menuHost = requireActivity()

        //MenuHost에 추가 할 MenuProvider
        menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_playlist, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                Log.d("툴바", "PlaylistFragment")
                when(menuItem.itemId) {
                    R.id.action_add_playlist -> {
                        Log.d("Dialog", "다이얼로그!")
                        val editText = EditText(mContext)
                        val dialog = AlertDialog.Builder(mContext)
                            .setTitle("플레이리스트 추가")
                            .setView(editText)
                            .setPositiveButton("확인") { dlg, id ->
                                if(editText.text.toString().isNotEmpty()) {
                                    val intent = Intent(mContext, SelectMusicActivity::class.java)
                                    intent.putExtra("no", getNewPlaylistNo())
                                    intent.putExtra("name", editText.text.toString())
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(mContext, "플레이리스트 이름을 입력해 주세요.", Toast.LENGTH_LONG).show()
                                }
                            }.setNegativeButton("취소") { dlg, id-> }

                        dialog.show()
                    }
                }

                return true
            }
        }

        //MenuHost에 MenuProvider를 추가, 프래그먼트가 RESUMED 상태에 진입하면 메뉴가 생성되며 RESUMED 상태에서 떨어지면 메뉴가 사라진다.
        menuHost.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    //프래그먼트가 화면에서 완전히 가려지면 onStop()까지만 호출됨
    //즉, 프래그먼트가 화면에 다시 보여지면 onStart()부터 호출되므로 onStart()에서 showPlaylist()를 호출해 플레이리스트 목록을 띄운다!
    override fun onStart() {
        super.onStart()
        showPlaylist() //플레이리스트 목록을 프래그먼트에 출력
    }

    override fun onDestroy() {
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun doEvent(event: Event) {
        when(event.getEvent()) {
            "STOP" -> {
                adapter?.notifyItemRangeChanged(0, adapter?.playlistList?.size ?: 0)
            }
            "OPEN_PLAYLIST_DETAILED" -> { //플레이리스트의 상세 곡 목록이 보여질 때
                menuHost.removeMenuProvider(menuProvider)
            }
            "CLOSE_PLAYLIST_DETAILED" -> { //플레이리스트의 상세 곡 목록이 닫힐 때
                menuHost.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
            }
        }
    }

    private fun getNewPlaylistNo(): Long { //플레이리스트의 번호(ID) 생성
        if(playlistList.isEmpty())
            return 1
        return playlistList.last().no + 1
    }

    private fun showPlaylist() {
        playlistList = helper.playlistDao().getAllPlaylist()

        //어댑터 생성 및 리사이클러뷰 설정
        if(adapter == null) {
            adapter = PlaylistAdapter(mContext)
            adapter!!.playlistList = playlistList
            adapter!!.helper = helper
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(mContext)
        } else {
            adapter!!.playlistList = playlistList
            adapter!!.notifyDataSetChanged()
        }
    }
}