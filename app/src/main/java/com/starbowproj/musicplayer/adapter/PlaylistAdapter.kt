package com.starbowproj.musicplayer.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.starbowproj.musicplayer.MusicPlayer
import com.starbowproj.musicplayer.R
import com.starbowproj.musicplayer.activity.MainActivity
import com.starbowproj.musicplayer.databinding.PlaylistItemRecyclerBinding
import com.starbowproj.musicplayer.fragment.DetailedPlaylistFragment
import com.starbowproj.musicplayer.room.Playlist
import com.starbowproj.musicplayer.room.PlaylistRoomHelper

class PlaylistAdapter(var mContext: Context) : RecyclerView.Adapter<PlaylistAdapter.Holder>() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }
    var helper: PlaylistRoomHelper? = null

    var playlistList = mutableListOf<Playlist>()

    //------------------------------------------------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = PlaylistItemRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val playlist = playlistList[position]
        holder.setPlaylist(playlist)
        holder.setBorder()
    }

    override fun getItemCount(): Int {
        return playlistList.size
    }
    //------------------------------------------------------------------------------------

    inner class Holder(val binding: PlaylistItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root) {
        private var mPlaylist: Playlist? = null

        init {
            itemView.setOnClickListener { //뷰홀더를 클릭 시
                val detailedPlaylistFragment = DetailedPlaylistFragment()
                val bundle = Bundle()

                val playlist = mPlaylist
                bundle.putSerializable("currentPlaylist", playlist)
                detailedPlaylistFragment.arguments = bundle

                val activity = mContext as MainActivity
                val transaction = activity.supportFragmentManager.beginTransaction()
                transaction.add(R.id.frameLayout, detailedPlaylistFragment)
                transaction.addToBackStack(null)
                transaction.commit()
            }

            binding.btnDeletePlaylist.setOnClickListener { //삭제 버튼을 클릭 시
                try {
                    //삭제하기 전에 삭제할것인지 확인하는 다이얼로그를 먼저 보여준다.
                    val dialog = AlertDialog.Builder(mContext)
                        .setTitle("플레이리스트 삭제")
                        .setMessage("정말로 ${mPlaylist?.name} 플레이리스트를 삭제하시겠습니까?")
                        .setPositiveButton("확인") { dlg, id ->
                            deletePlaylist() //확인 버튼을 누르면 삭제 작업 진행
                        }.setNegativeButton("취소") { dlg, id -> }

                    dialog.show()
                } catch (e: Exception) { //예외발생시 에러 메시지 출력
                    e.printStackTrace()
                    Toast.makeText(mContext, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun setPlaylist(playlist: Playlist) {
            this.mPlaylist = playlist
            binding.playlistName.text = playlist.name
        }

        //해당 뷰홀더에 지정된 플레이리스트를 삭제하는 메서드
        private fun deletePlaylist() {
            try {
                if(musicPlayer.getPlaylistId() == mPlaylist?.no) {
                    musicPlayer.stop()
                }

                playlistList.removeAt(this.adapterPosition)
                notifyItemRangeRemoved(this.adapterPosition, 1)

                //삭제할 플레이 리스트의 곡 목록 얻기
                val deleteMusicList = helper?.playlistMusicDao()?.getAllMusic(mPlaylist?.no!!) ?: listOf()
                for (playlistMusic in deleteMusicList) {
                    helper?.playlistMusicDao()?.deleteMusic(playlistMusic) //곡 전부 DB에서 삭제
                }
                helper?.playlistDao()?.delete(mPlaylist!!) //플레이 리스트를 DB에서 제거


            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(binding.root.context, "이미 삭제됐거나 삭제할 수 없는 플레이리스트입니다.", Toast.LENGTH_LONG).show()
            }
        }

        fun setBorder() {
            if((mPlaylist?.no ?: -1) == musicPlayer.getPlaylistId()) {
                itemView.background = AppCompatResources.getDrawable(mContext, R.drawable.border_now_playlist)
            } else {
                itemView.background = null
            }
        }
    }
}

