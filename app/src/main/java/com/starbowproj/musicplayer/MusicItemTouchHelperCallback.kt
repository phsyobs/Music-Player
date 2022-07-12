package com.starbowproj.musicplayer

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.starbowproj.musicplayer.adapter.MusicAdapter
import java.util.*

class MusicItemTouchHelperCallback(val adapter: MusicAdapter): ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN //드래그 액션에 대한 플래그
        //var swipeFlags: Int //스와이프 액션에 대한 플래그
        return makeMovementFlags(dragFlags, 0) //이동 플래그를 만들어서 반환 (사용하지 않는 액션의 경우 0을 넘겨주면 된다.)
    }

    //움직임이 발생 시 어떤 동작을 할것인지 구현하는 곳
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition //adapterPosition을 통해 뷰 홀더의 위치를 얻을 수 있다.
        val toPosition = target.adapterPosition
        Collections.swap(adapter.musicList, fromPosition, toPosition) //어댑터의 musicList에 변경사항을 반영
        adapter.notifyItemMoved(fromPosition, toPosition) //아이템뷰가 이동되었음을 알림
        return true
    }

    //Swipe 동작이 발생 시 어떤 동작을 할것인지 구현하는 곳
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }
}