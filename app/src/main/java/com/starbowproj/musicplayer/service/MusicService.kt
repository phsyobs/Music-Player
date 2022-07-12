package com.starbowproj.musicplayer.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.squareup.otto.Subscribe
import com.starbowproj.musicplayer.*
import com.starbowproj.musicplayer.activity.MusicActivity
import com.starbowproj.musicplayer.event.Event
import com.starbowproj.musicplayer.event.EventBus
import java.io.IOException


class MusicService : Service() {
    val musicPlayer by lazy { MusicPlayer.getInstance() }

    companion object {
        const val CHANNEL_ID = "MusicChannel" //알림 채널 ID

        const val PLAYPAUSE = "com.starbowproj.musicplayer.PLAYPAUSE" //Notification에서 재생/일시정지 버튼을 누를 시
        const val SKIP_PREV = "com.starbowproj.musicplayer.SKIP_PREV" //Notification에서 이전 곡 버튼을 누를 시
        const val SKIP_NEXT = "com.starbowproj.musicplayer.SKIP_NEXT" //Notification에서 다음 곡 버튼을 누를 시
        const val CLOSE = "com.starbowproj.musicplayer.CLOSE" //닫기 버튼을 누를 시
    }

    private lateinit var remoteNotificationLayout: RemoteViews
    private lateinit var remoteNotificationExtendedLayout: RemoteViews

    override fun onCreate() {
        super.onCreate()
        EventBus.getInstance().register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel() //알림 채널 생성

        //Notification에 사용할 RemoteViews 생성
        remoteNotificationLayout = RemoteViews(this.packageName, R.layout.notification_player)
        remoteNotificationExtendedLayout = RemoteViews(this.packageName, R.layout.notification_player_extended)

        Log.d("MusicServiceAction", "action = ${intent?.action}")

        when(intent?.action) {
            null -> {
                val notification = createNotification() //Notification 생성
                startForeground(1, notification) //포어그라운드에서 실행
            }
            PLAYPAUSE -> { //Notification에서 재생/일시정지 버튼을 누를 시
                if(musicPlayer.isPlaying()) { musicPlayer.pause() } else { musicPlayer.start() }
            }
            SKIP_PREV -> { //Notification에서 이전 곡 버튼을 누를 시
                musicPlayer.skipPrev() //MusicActivity의 btnPrev의 클릭 리스너를 동작시킴
            }
            SKIP_NEXT -> { //Notification에서 다음 곡 버튼을 누를 시
                musicPlayer.skipNext() //MusicActivity의 btnNext의 클릭 리스너를 동작시킴
            }
            CLOSE -> { //Notification에서 닫기 버튼 누를 시
                stopSelf()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    //서비스가 완전히 종료되면
    override fun onDestroy() {
        musicPlayer.stop() //플레이어 중단
        EventBus.getInstance().unregister(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    @Subscribe
    fun doEvent(event: Event) {
        when (event.getEvent()) {
            //다른 곳에서 플레이어가 정지될 수 있기 때문에 넣어줬음
            "STOP" -> { stopSelf() }
        }
    }

    //알림 채널을 생성하는 메서드
    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "musicPlayerChannel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    //알림을 생성하는 메서드
    private fun createNotification(): Notification {
        //각 action에 해당하는 PendingIntent 생성
        val prevIntent = Intent(this, MusicService::class.java).run {
            action = SKIP_PREV
            PendingIntent.getService(this@MusicService, 0, this, PendingIntent.FLAG_IMMUTABLE)
        }
        val playIntent = Intent(this, MusicService::class.java).run {
            action = PLAYPAUSE
            PendingIntent.getService(this@MusicService, 1, this, PendingIntent.FLAG_IMMUTABLE)
        }
        val nextIntent = Intent(this, MusicService::class.java).run {
            action = SKIP_NEXT
            PendingIntent.getService(this@MusicService, 2, this, PendingIntent.FLAG_IMMUTABLE)
        }
        val closeIntent = Intent(this, MusicService::class.java).run {
            action = CLOSE
            PendingIntent.getService(this@MusicService, 3, this, PendingIntent.FLAG_IMMUTABLE)
        }
        val notiTouchIntent = Intent(this, MusicActivity::class.java).run {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            PendingIntent.getActivity(this@MusicService, 4, this, PendingIntent.FLAG_IMMUTABLE)
        }

        //클릭 리스너를 달아준다. 이 때, setOnClickPendingIntent()를 이용해서 달아준다
        remoteNotificationLayout.setOnClickPendingIntent(R.id.btnNotiPrev, prevIntent)
        remoteNotificationLayout.setOnClickPendingIntent(R.id.btnNotiPlay, playIntent)
        remoteNotificationLayout.setOnClickPendingIntent(R.id.btnNotiNext, nextIntent)

        remoteNotificationExtendedLayout.setOnClickPendingIntent(R.id.btnNotiExtendedPrev, prevIntent)
        remoteNotificationExtendedLayout.setOnClickPendingIntent(R.id.btnNotiExtendedPlay, playIntent)
        remoteNotificationExtendedLayout.setOnClickPendingIntent(R.id.btnNotiExtendedNext, nextIntent)
        remoteNotificationExtendedLayout.setOnClickPendingIntent(R.id.btnNotiExtendedClose, closeIntent)

        val music = musicPlayer.getCurrentMusic()

        //알림에 음원 정보를 세팅한다. 정확히는 알림으로 사용할 레이아웃에 음원 정보를 세팅
        setMusicInNotification(music)

        //알림 생성
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.music_note)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteNotificationLayout)
            .setCustomBigContentView(remoteNotificationExtendedLayout)
            .setContentIntent(notiTouchIntent)
            .build()

        return notification
    }

    private fun setMusicInNotification(music: Music?) {
        var bitmap: Bitmap? = null

        try {
            bitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { //P 이상인 경우
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, music?.getAlbumUri()!!))
            } else { //그 이하인 경우
                //API 29부터 MediaStore.Images.Media.getBitmap()은 deprecated 됨
                MediaStore.Images.Media.getBitmap(contentResolver, music?.getAlbumUri()!!)
            }
        } catch (e: IOException) { //음원의 앨범아트 Uri에 해당하는 파일이 없는 경우 예외가 발생하고 이 경우 bitmap에 null을 담는다.
            e.printStackTrace()
            bitmap = null
        } finally {
            if (bitmap != null) { //null이 아닌경우 다시말해 앨범아트 Uri에 파일이 있는 경우
                remoteNotificationLayout.setImageViewBitmap(R.id.imageNotiAlbum, bitmap)
                remoteNotificationExtendedLayout.setImageViewBitmap(
                    R.id.imageNotiExtendedAlbum,
                    bitmap
                )
            } else { //앨범아트 Uri에 파일이 없는 경우 기본 앨범아트를 사용
                remoteNotificationLayout.setImageViewResource(
                    R.id.imageNotiAlbum,
                    R.drawable.default_album_art
                )
                remoteNotificationExtendedLayout.setImageViewResource(
                    R.id.imageNotiExtendedAlbum,
                    R.drawable.default_album_art
                )
            }
        }

        //시작/일시정지 버튼 세팅
        if(musicPlayer.isPlaying()) {
            remoteNotificationLayout.setImageViewResource(R.id.btnNotiPlay, R.drawable.pause)
            remoteNotificationExtendedLayout.setImageViewResource(
                R.id.btnNotiExtendedPlay,
                R.drawable.pause
            )
        } else {
            remoteNotificationLayout.setImageViewResource(R.id.btnNotiPlay, R.drawable.play_arrow)
            remoteNotificationExtendedLayout.setImageViewResource(
                R.id.btnNotiExtendedPlay,
                R.drawable.play_arrow
            )
        }

        //텍스트뷰와 나머지 이미지버튼 세팅
        remoteNotificationLayout.setTextViewText(R.id.textNotiTitle, music?.title)
        remoteNotificationLayout.setTextViewText(R.id.textNotiArtist, music?.artist)
        remoteNotificationLayout.setImageViewResource(R.id.btnNotiPrev, R.drawable.skip_previous)
        remoteNotificationLayout.setImageViewResource(R.id.btnNotiNext, R.drawable.skip_next)

        remoteNotificationExtendedLayout.setTextViewText(R.id.textNotiExtendedTitle, music?.title)
        remoteNotificationExtendedLayout.setTextViewText(R.id.textNotiExtendedArtist, music?.artist)
        remoteNotificationExtendedLayout.setImageViewResource(
            R.id.btnNotiExtendedPrev,
            R.drawable.skip_previous
        )
        remoteNotificationExtendedLayout.setImageViewResource(
            R.id.btnNotiExtendedNext,
            R.drawable.skip_next
        )
        remoteNotificationExtendedLayout.setImageViewResource(
            R.id.btnNotiExtendedClose,
            R.drawable.close_small
        )
    }
}