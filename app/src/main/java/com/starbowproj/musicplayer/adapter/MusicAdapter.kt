package com.starbowproj.musicplayer.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.starbowproj.musicplayer.*
import com.starbowproj.musicplayer.activity.MusicActivity
import com.starbowproj.musicplayer.databinding.MusicItemRecyclerBinding
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import com.starbowproj.musicplayer.room.Playlist
import com.starbowproj.musicplayer.room.PlaylistMusic
import com.starbowproj.musicplayer.room.PlaylistRoomHelper

class MusicAdapter(var mContext: Context): RecyclerView.Adapter<MusicAdapter.Holder>() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }
    var helper: PlaylistRoomHelper? = null

    var playlist: Playlist? = null //현재 플레이리스트 (사용 안할 시 null)
    var musicList = mutableListOf<Music>() //전체 음원 or 플레이리스트에 속해있는 음원 리스트

    var useCheckbox = false //뷰홀더에서 체크박스를 보여줄 것인지를 확인하는 변수
    var checkList = mutableListOf<Boolean>() //각 음원의 체크박스의 체크여부 (체크박스 사용시)

    var useSortHandler = false //뷰홀더에서 정렬 핸들러를 보여줄 것인지를 확인하는 변수
    var mStartDragHolder: OnStartDragHolder? = null //btnSortHandler을 눌러서 드래그시 발생할 리스너

    interface OnStartDragHolder { //btnSortHandler을 눌러서 드래그시 발생할 이벤트를 구현할 리스너
        fun onStartDrag(holder: Holder)
    }

    //------------------------------------------------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = MusicItemRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val music = musicList[position]
        holder.setMusic(music)
        //setButton()을 여기에 넣어주는 이유는 notifyDataSetChanged()를 사용하면 onCreateViewHolder()를 거치지 않기 때문이다!!
        holder.setButton()
        holder.setBorder()
    }

    override fun getItemCount(): Int {
        return musicList.size
    }
    //------------------------------------------------------------------------------------

    inner class Holder(val binding: MusicItemRecyclerBinding): RecyclerView.ViewHolder(binding.root) {
        private var mMusic: Music? = null

        init {
            binding.imageView.clipToOutline = true

            if (useCheckbox) { //체크박스를 사용하는 경우
                binding.checkBox.visibility = View.VISIBLE

                binding.checkBox.setOnCheckedChangeListener { compoundButton, isChecked ->
                    checkList[this.adapterPosition] = isChecked
                }
            } else {
                binding.checkBox.visibility = View.GONE
            }

            //아이템 뷰를 클릭하면 인텐트에 뮤직리스트와 클릭된 아이템뷰의 위치값을 담아서 MusicActivity에 전달
            //체크박스와 정렬 핸들러를 둘다 사용하지 않는 경우에만 클릭되도록 한다.
            //인텐트에 객체를 전달하기 위해서는 넘겨주는 클래스에 추가 작업이 필요함 (Music 클래스 참고)
            itemView.setOnClickListener {
                if (!useCheckbox && !useSortHandler) {
                    musicPlayer.setMusicList(musicList)

                    val intent = Intent(mContext, MusicActivity::class.java)
                    intent.putExtra("playlist", this@MusicAdapter.playlist)
                    intent.putExtra("curPosition", this.adapterPosition)
                    val activity = mContext as AppCompatActivity
                    activity.startActivity(intent)
                }
            }

            binding.run {
                btnDeleteMusic.setOnClickListener { //삭제 버튼
                    val music = musicList[this@Holder.adapterPosition]
                    val deleteMusic = PlaylistMusic(
                        music.id,
                        playlist!!.no,
                        this@Holder.adapterPosition,
                        music.title,
                        music.artist,
                        music.albumId,
                        music.duration
                    )

                    helper?.playlistMusicDao()?.deleteMusic(deleteMusic) //데이터베이스에서 곡 삭제
                    musicList.removeAt(this@Holder.adapterPosition) //음원 목록에서 제거
                    notifyItemRangeRemoved(this@Holder.adapterPosition, 1)
                    for (i in 0 until musicList.size) {
                        helper?.playlistMusicDao()?.changeOrder(playlist!!.no, musicList[i].id, i) //데이터베이스의 order값들을 수정
                    }

                    if (musicPlayer.getCurrentMusicId() == music.id) { //삭제하고자하는 음원이 현재 재생중인 음원이라면
                        musicPlayer.stop() //플레이어 중단
                    }
                    musicPlayer.deleteMusic(music) //musicPlayer에서 사용하는 musicList에서도 해당 음원 제거

                    EventBus.getInstance().post(Event("CHANGE_PLAYLIST_NUM_MUSIC", musicList.size))
                }

                //warning을 없애기 위해 btnSortHandler를 TouchableButton으로 생성 (performClick() 메서드가 오버라이드된 버튼의 하위 클래스이기 때문)
                //또한 onTouch() 구현부에서 performClick()을 사용함 (이것 또한 warning을 없애기 위함)
                btnSortHandler.setOnTouchListener { view, motionEvent ->  //곡 정렬 핸들러
                    when(motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN -> mStartDragHolder?.onStartDrag(this@Holder)
                        MotionEvent.ACTION_UP -> view.performClick()
                    }
                    false
                }
            }
        }

        fun setMusic(music: Music) { //뷰홀더에 음원 데이터를 바인딩
            this.mMusic = music

            binding.title.text = music.title

            binding.artist.text = music.artist
            binding.imageView.setImageURI(music.getAlbumUri())
            if(binding.imageView.drawable == null) {
                binding.imageView.setImageResource(R.drawable.default_album_art)
            }
        }

        fun setButton() { //플레이리스트 사용 여부와 useSortHandler에 따라 화면에 버튼을 띄운다
            if(playlist != null) { //플레이리스트에서 사용시
                if (useSortHandler) {
                    binding.btnSortHandler.visibility = View.VISIBLE
                    binding.btnDeleteMusic.visibility = View.GONE
                } else {
                    binding.btnDeleteMusic.visibility = View.VISIBLE
                    binding.btnSortHandler.visibility = View.GONE
                }
            } else { //플레이리스트에서 사용하는 경우가 아니면
                binding.btnDeleteMusic.visibility = View.GONE
                binding.btnSortHandler.visibility = View.GONE
            }
        }

        fun setBorder() { //현재 재생중인 음원의 뷰 홀더를 파란색 테두리를 이용해 표시
            if(!musicPlayer.isStop() && (mMusic?.id ?: "") == musicPlayer.getCurrentMusicId()) {
                itemView.background = AppCompatResources.getDrawable(mContext, R.drawable.border_now_play)
            } else {
                itemView.background = null
            }
        }
    }
}

