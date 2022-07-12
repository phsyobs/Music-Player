package com.starbowproj.musicplayer.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.*
import com.starbowproj.musicplayer.activity.MainActivity
import com.starbowproj.musicplayer.activity.SelectMusicActivity
import com.starbowproj.musicplayer.adapter.MusicAdapter
import com.starbowproj.musicplayer.databinding.FragmentDetailedPlaylistBinding
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.room.Playlist
import com.starbowproj.musicplayer.room.PlaylistRoomHelper

class DetailedPlaylistFragment : Fragment(), MusicAdapter.OnStartDragHolder {
    private val musicPlayer by lazy { MusicPlayer.getInstance() }

    private lateinit var binding: FragmentDetailedPlaylistBinding
    private lateinit var mContext: Context
    private lateinit var helper: PlaylistRoomHelper
    private var adapter: MusicAdapter? = null //액티비티에서 사용하는 리사이클러뷰 어댑터

    private lateinit var currentPlaylist: Playlist //현재 선택된 플레이리스트
    private var musicList: MutableList<Music> = mutableListOf() //선택된 플레이리스트의 음원 리스트

    private lateinit var mItemTouchHelper: ItemTouchHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        EventBus.getInstance().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailedPlaylistBinding.inflate(inflater, container, false)
        binding.recyclerView.setEmptyView(binding.textEmptyDetailedPlaylist)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helper = PlaylistRoomHelper.getHelper(mContext)

        //DetailedPlaylistFragment는 MainActivity의 액션 바를 사용 중
        //MainActivity의 액션바를 받아와서 뒤로가기 버튼을 활성화
        val actionBar = (mContext as MainActivity).supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detailed_playlist, menu)
                //메뉴를 생성할 때 정렬 핸들러 사용 여부에 따라 아이콘을 변경하거나 숨긴다.
                menu.getItem(0).isVisible = !(adapter?.useSortHandler ?: false)
                menu.getItem(1).setIcon(if(adapter?.useSortHandler == false) R.drawable.sort else R.drawable.done)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                Log.d("툴바", "DetailedPlaylistFragment")
                when(menuItem.itemId) {
                    android.R.id.home -> { //뒤로가기 버튼을 누를 시
                        EventBus.getInstance().post(Event("CLOSE_PLAYLIST_DETAILED"))
                        EventBus.getInstance().unregister(this)
                        val activity = mContext as MainActivity
                        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        val transaction = activity.supportFragmentManager.beginTransaction()
                        transaction.remove(this@DetailedPlaylistFragment)
                        transaction.commit()
                    }
                    R.id.action_add_music -> { //음원 추가 버튼을 누를 시
                        val intent = Intent(mContext, SelectMusicActivity::class.java)
                        intent.putExtra("no", currentPlaylist.no)
                        intent.putExtra("name", currentPlaylist.name)
                        startActivity(intent)
                    }
                    R.id.action_change_music_position -> { //음원 위치 변경 버튼을 누를 시
                        adapter?.let {
                            if (!it.useSortHandler) { //핸들러 사용여부가 false였다면
                                it.useSortHandler = true //핸들러 사용 여부 변경 후
                                menuHost.invalidateMenu() //메뉴 갱신
                                EventBus.getInstance().post(Event("START_SORTING"))
                            } else { //핸들러 사용여부가 true였다면
                                it.useSortHandler = false //핸들러 사용 여부 변경 후
                                menuHost.invalidateMenu() //메뉴 갱신
                                EventBus.getInstance().post(Event("FINISH_SORTING"))

                                for (i in 0 until musicList.size) {
                                    helper.playlistMusicDao().changeOrder(currentPlaylist.no, musicList[i].id, i)
                                }
                                musicPlayer.setMusicList(musicList) //정렬 완료 후 플레이어에 새로운 뮤직리스트로 세팅
                            }
                            it.notifyItemRangeChanged(0, musicList.size)
                        }
                    }
                }

                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onStart() {
        //onStart()에서 보여줘야하는 플레이리스트를 읽어온 후 곡 목록을 보여준다.
        //그래야 SelectMusicActivity에서 곡 추가 후 변경사항을 리사이클러뷰에 바로 반영할 수 있다.
        currentPlaylist = arguments?.getSerializable("currentPlaylist") as Playlist
        EventBus.getInstance().post(Event("OPEN_PLAYLIST_DETAILED", currentPlaylist.name))
        Log.d("플레이리스트", "playlist = ${currentPlaylist.name}")
        showPlaylist(currentPlaylist)
        if(musicPlayer.getPlaylistId() == currentPlaylist.no) { //플레이어에서 재생 중인 플레이리스트와 음원이 추가된 플레이리스트가 일치하면
            musicPlayer.setMusicList(musicList) //플레이어에 재생할 음원을 다시 세팅해준다
        }
        super.onStart()
    }

    //스마트폰에 기본적으로 있는 뒤로가기 버튼을 누르면 onDestroy()를 거침
    //트랜잭션을 이용해서 종료시키면 onDestroy()를 거치지 않음 (이유는 모름)
    override fun onDestroy() {
        Log.d("툴바 ", "DESTROYED")
        EventBus.getInstance().post(Event("CLOSE_PLAYLIST_DETAILED"))
        (mContext as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun doEvent(event: Event) {
        when(event.getEvent()) {
            "PLAY_NEW_MUSIC", "STOP" -> {
                adapter?.notifyItemRangeChanged(0, adapter?.musicList?.size ?: 0)
            }
        }
    }

    override fun onStartDrag(holder: MusicAdapter.Holder) {
        mItemTouchHelper.startDrag(holder)
    }

    //선택한 플레이리스트 및 해당 정보를 보여줌 (플레이리스트 이름, 들어있는 곡의 수, 곡 목록)
    private fun showPlaylist(playlist: Playlist) {
        musicList = getMusicList(playlist)

        EventBus.getInstance().post(Event("CHANGE_PLAYLIST_NUM_MUSIC", musicList.size))

        if (adapter == null) { //생성된 어댑터가 없는경우 생성하고 세팅한다.
            adapter = MusicAdapter(mContext)
            adapter?.let {
                it.mStartDragHolder = this@DetailedPlaylistFragment
                it.helper = helper
                it.playlist = playlist
                it.musicList = musicList
            }

            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(mContext)

            //ItemTouchHelper 객체 생성 후 리사이클러뷰에 붙여준다
            val callback = MusicItemTouchHelperCallback(adapter!!)
            mItemTouchHelper = ItemTouchHelper(callback)
            mItemTouchHelper.attachToRecyclerView(binding.recyclerView)
        } else {
            adapter!!.let {
                it.musicList = musicList
                it.notifyDataSetChanged()
            }
        }
    }

    //선택한 플레이리스트에 들어있는 음원 목록을 반환 (MutableList<Music>으로 반환)
    private fun getMusicList(playlist: Playlist): MutableList<Music> {
        val playlistMusicList = helper.playlistMusicDao().getAllMusic(playlist.no)
        val musicList = mutableListOf<Music>()
        for (playlistMusic in playlistMusicList) {
            val music = Music(
                playlistMusic.id,
                playlistMusic.title,
                playlistMusic.artist,
                playlistMusic.albumId,
                playlistMusic.duration
            )
            musicList.add(music)
        }

        return musicList
    }
}