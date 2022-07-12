package com.starbowproj.musicplayer

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

//AppCompatImageButton을 상속 받아서 만든 커스텀 이미지버튼
class TouchableButton: AppCompatImageButton {
    //생성자
    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    //performClick()을 오버라이드
    //performClick()은 해당 메서드를 호출한 뷰의 클릭 이벤트를 발생시키는 View의 메서드이다.
    override fun performClick(): Boolean {
        return super.performClick()
    }
}