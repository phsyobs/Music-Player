package com.starbowproj.musicplayer

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView

//리사이클러뷰에 띄울 아이템이 없을 시 emptyView를 띄우는 커스텀 뷰
class RecyclerViewWithEmptyView: RecyclerView {
    private var emptyView: View? = null //아이템이 없을 시 띄울 뷰

    //AdapterDataObserver는 어댑터의 변경사항을 확인하기 위해 사용하는 클래스
    //어댑터를 갱신할 때 어떤 메서드가 사용될지 모르므로 필요한 모든 메서드를 오버라이드 한다!
    private var emptyObserver = object: AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            Log.d("리사이클러뷰", "onChanged")
            changeView()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            Log.d("리사이클러뷰", "onItemRangeChanged")
            changeView()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            Log.d("리사이클러뷰", "onItemRangeInserted")
            changeView()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            Log.d("리사이클러뷰", "onItemRangeMoved")
            changeView()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            Log.d("리사이클러뷰", "onItemRangeRemoved")
            changeView()
        }
    }
    
    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        if(adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver)
            emptyObserver.onChanged()
        }
    }
    
    fun setEmptyView(view: View) {
        emptyView = view
    }

    fun changeView() {
        val adapter: Adapter<*>? = adapter
        Log.d("리사이클러뷰", "itemCount: ${adapter?.itemCount}, isEmptyViewNotNull: ${if(emptyView != null) "YES" else "NO"}")
        if(adapter != null && emptyView != null) {
            if(adapter.itemCount == 0) { //아이템이 없으면
                emptyView!!.visibility = View.VISIBLE //emptyView를 띄운다
                this@RecyclerViewWithEmptyView.visibility = View.GONE
            }
            else {
                emptyView!!.visibility = View.GONE
                this@RecyclerViewWithEmptyView.visibility = View.VISIBLE
            }
        }
    }

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
}