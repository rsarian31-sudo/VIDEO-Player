package com.wajid.videoplayer;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Locale;

public class MusicPlayerActivity extends AppCompatActivity {
    private static final int BLUE=0xFF64B5F6;
    private MediaController controller;
    private ListenableFuture<MediaController> future;
    private TextView title, artist, playButton, position, duration;
    private SeekBar seek;
    private boolean userSeeking=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){public void run(){refresh();handler.postDelayed(this,400);}};
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        FrameLayout base=new FrameLayout(this);base.setBackgroundColor(Color.BLACK);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(8),dp(22),dp(18));
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(-1,-1);base.addView(root,rp);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=txt("‹",38,Color.WHITE,false);top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(52)));back.setOnClickListener(v->finish());
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(txt("NOW PLAYING",11,BLUE,true));labels.addView(txt("WAJID MUSIC",18,Color.WHITE,true));top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView more=txt("⋮",28,Color.WHITE,false);more.setGravity(Gravity.CENTER);top.addView(more,new LinearLayout.LayoutParams(dp(34),dp(48)));
        root.addView(top);
        LinearLayout artArea=new LinearLayout(this);artArea.setGravity(Gravity.CENTER);artArea.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams artLp=new LinearLayout.LayoutParams(-1,0,1);artLp.topMargin=dp(12);artLp.bottomMargin=dp(12);root.addView(artArea,artLp);
        FrameLayout art=new FrameLayout(this);GradientDrawable artBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF202A3A,0xFF111319,0xFF273B55});artBg.setCornerRadius(dp(22));art.setBackground(artBg);
        TextView vinyl=txt("♫",104,Color.WHITE,true);vinyl.setGravity(Gravity.CENTER);art.addView(vinyl,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams coverLp=new LinearLayout.LayoutParams(-1,-1);coverLp.width=-1;coverLp.height=0;coverLp.weight=1;coverLp.gravity=Gravity.CENTER;coverLp.leftMargin=dp(10);coverLp.rightMargin=dp(10);artArea.addView(art,coverLp);
        title=txt("Choose a song",22,Color.WHITE,true);title.setGravity(Gravity.CENTER);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(0,dp(12),0,dp(4));root.addView(title);
        artist=txt("WAJID MUSIC",13,0xFFB7BBC5,false);artist.setGravity(Gravity.CENTER);root.addView(artist);
        LinearLayout timeline=new LinearLayout(this);timeline.setGravity(Gravity.CENTER_VERTICAL);timeline.setPadding(0,dp(16),0,dp(8));
        position=txt("00:00",12,Color.WHITE,false);timeline.addView(position);
        seek=new SeekBar(this);seek.setMax(1000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(BLUE));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(BLUE));timeline.addView(seek,new LinearLayout.LayoutParams(0,dp(36),1));
        duration=txt("00:00",12,0xFFB7BBC5,false);timeline.addView(duration);root.addView(timeline);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){if(from)position.setText(clock(controller==null?0:(controller.getDuration()*p/1000)));}public void onStartTrackingTouch(SeekBar s){userSeeking=true;}public void onStopTrackingTouch(SeekBar s){if(controller!=null&&controller.getDuration()>0)controller.seekTo(controller.getDuration()*s.getProgress()/1000);userSeeking=false;}});
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setPadding(0,dp(12),0,dp(8));
        TextView prev=txt("⏮",31,Color.WHITE,true);prev.setGravity(Gravity.CENTER);controls.addView(prev,new LinearLayout.LayoutParams(0,dp(66),1));prev.setOnClickListener(v->{if(controller!=null)controller.seekToPreviousMediaItem();});
        playButton=txt("▶",36,Color.WHITE,true);playButton.setGravity(Gravity.CENTER);GradientDrawable circle=new GradientDrawable();circle.setColor(BLUE);circle.setShape(GradientDrawable.OVAL);playButton.setBackground(circle);controls.addView(playButton,new LinearLayout.LayoutParams(dp(76),dp(76)));playButton.setOnClickListener(v->{if(controller!=null){if(controller.isPlaying())controller.pause();else controller.play();refresh();}});
        TextView next=txt("⏭",31,Color.WHITE,true);next.setGravity(Gravity.CENTER);controls.addView(next,new LinearLayout.LayoutParams(0,dp(66),1));next.setOnClickListener(v->{if(controller!=null)controller.seekToNextMediaItem();});
        root.addView(controls);
        TextView hint=txt("Playback continues when you leave the app or lock the screen",11,0xFF9297A4,false);hint.setGravity(Gravity.CENTER);hint.setPadding(0,dp(10),0,0);root.addView(hint);
        setContentView(base);
        SessionToken token=new SessionToken(this,new ComponentName(this,MusicPlaybackService.class));
        future=new MediaController.Builder(this,token).buildAsync();
        future.addListener(()->{try{controller=future.get();controller.addListener(new Player.Listener(){@Override public void onMediaItemTransition(MediaItem item,int reason){refresh();}@Override public void onIsPlayingChanged(boolean playing){refresh();}});ArrayList<Uri> uris=getIntent().getParcelableArrayListExtra("music_uris");ArrayList<String> names=getIntent().getStringArrayListExtra("music_titles");int idx=getIntent().getIntExtra("music_index",0);if(uris!=null&&!uris.isEmpty()){ArrayList<MediaItem> items=new ArrayList<>();for(int i=0;i<uris.size();i++){String n=names!=null&&i<names.size()?names.get(i):"Audio";items.add(new MediaItem.Builder().setUri(uris.get(i)).setMediaMetadata(new MediaMetadata.Builder().setTitle(n).setArtist("WAJID MUSIC").build()).build());}controller.setMediaItems(items,Math.max(0,Math.min(idx,items.size()-1)),0);controller.prepare();controller.play();}refresh();}catch(Exception ignored){}},ContextCompat.getMainExecutor(this));
    }
    private void refresh(){if(controller==null)return;MediaItem item=controller.getCurrentMediaItem();if(item!=null&&item.mediaMetadata.title!=null)title.setText(item.mediaMetadata.title.toString());if(controller.isPlaying()){playButton.setText("Ⅱ");}else{playButton.setText("▶");}long pos=Math.max(0,controller.getCurrentPosition()),dur=controller.getDuration();position.setText(clock(pos));duration.setText(dur>0?clock(dur):"00:00");if(!userSeeking)seek.setProgress(dur>0?(int)(pos*1000/dur):0);}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private String clock(long ms){if(ms<0)ms=0;long sec=ms/1000;return String.format(Locale.US,"%02d:%02d",sec/60,sec%60);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onStart(){super.onStart();handler.post(ticker);}
    @Override protected void onStop(){handler.removeCallbacks(ticker);super.onStop();}
    @Override protected void onDestroy(){if(future!=null)MediaController.releaseFuture(future);super.onDestroy();}
}
