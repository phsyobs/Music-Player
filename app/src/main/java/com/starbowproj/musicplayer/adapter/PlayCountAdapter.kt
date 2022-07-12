package com.starbowproj.musicplayer.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.starbowproj.musicplayer.*
import com.starbowproj.musicplayer.activity.MusicActivity
import com.starbowproj.musicplayer.databinding.PlayCountItemRecyclerBinding
import com.starbowproj.musicplayer.room.PlayCount
import com.starbowproj.musicplayer.room.Playlist

class PlayCountAdapter(var mContext: Context): RecyclerView.Adapter<PlayCountAdapter.Holder>() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }
    var playCountList = mutableListOf<PlayCount>()
    val RANKING_PLAYLIST = Playlist(-1000, "Ranking")

    //------------------------------------------------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = PlayCountItemRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val playCount = playCountList[position]
        holder.setPlayCount(playCount)
        holder.setMusic(playCount)
        holder.setBorder()
    }

    override fun getItemCount(): Int {
        return playCountList.size
    }
    //------------------------------------------------------------------------------------

    inner class Holder(val binding: PlayCountItemRecyclerBinding): RecyclerView.ViewHolder(binding.root) {
        var music: Music? = null

        init {
            binding.imagePlayCount.clipToOutline = true

            itemView.setOnClickListener {
                val intent = Intent(mContext, MusicActivity::class.java)
                val musicArray = ArrayList<Music>()

                for(i in 0 until playCountList.size) {
                    val playCount = playCountList[i]
                    val music = Music(
                        playCount.musicId,
                        playCount.musicTitle,
                        playCount.musicArtist,
                        playCount.albumId,
                        playCount.duration
                    )
                    musicArray.add(music)
                }

                musicPlayer.setMusicList(musicArray)

                intent.putExtra("curPosition", this.adapterPosition)
                intent.putExtra("playlist", RANKING_PLAYLIST)
                val activity = mContext as AppCompatActivity
                activity.startActivity(intent)
            }
        }

        fun setPlayCount(playCount: PlayCount) { //뷰홀더에 음원 정보에 재생 횟수 띄우기
            binding.run {
                textRank.text = mContext.getString(R.string.play_count_rank, this@Holder.adapterPosition + 1)
                setTextRankAttribute()

                textPlayCountTitle.text = playCount.musicTitle
                textPlayCountArtist.text = playCount.musicArtist
                val albumUri = Uri.parse("content://media/external/audio/albumart/" + playCount.albumId)
                imagePlayCount.setImageURI(albumUri)
                if(imagePlayCount.drawable == null) {
                    imagePlayCount.setImageResource(R.drawable.default_album_art)
                }
                textPlayCount.text = mContext.getString(R.string.play_count, playCount.playCount)
            }
        }

        fun setMusic(playCount: PlayCount) { //뷰홀더에서 띄우는 음원을 프로퍼티에 Music 객체로 저장
            val music = Music(
                playCount.musicId,
                playCount.musicTitle,
                playCount.musicArtist,
                playCount.albumId,
                playCount.duration
            )
            this.music = music
        }

        private fun setTextRankAttribute() { //순위에 따라 텍스트의 속성을 다르게 지정
            when(this.adapterPosition + 1) {
                1 -> {
                    binding.textRank.textSize = 36f
                    binding.textRank.setTextColor(ContextCompat.getColor(mContext, R.color.gold))
                }
                2 -> {
                    binding.textRank.textSize = 36f
                    binding.textRank.setTextColor(ContextCompat.getColor(mContext, R.color.silver))
                }
                3 -> {
                    binding.textRank.textSize = 36f
                    binding.textRank.setTextColor(ContextCompat.getColor(mContext, R.color.bronze))
                }
                in 4..10 -> {
                    binding.textRank.textSize = 24f
                    binding.textRank.setTextColor(ContextCompat.getColor(mContext, R.color.ranking_top_10))
                }
                else -> {
                    binding.textRank.textSize = 16f
                    binding.textRank.setTextColor(ContextCompat.getColor(mContext as AppCompatActivity,
                        androidx.recyclerview.R.color.secondary_text_default_material_light))
                }
            }
        }

        //현재 재생중인 음원의 뷰 홀더를 파란색 테두리를 이용해 표시
        fun setBorder() {
            if(!musicPlayer.isStop() && (music?.id ?: "") == musicPlayer.getCurrentMusicId()) {
                itemView.background = AppCompatResources.getDrawable(mContext, R.drawable.border_now_play)
            } else {
                itemView.background = null
            }
        }
    }
}

