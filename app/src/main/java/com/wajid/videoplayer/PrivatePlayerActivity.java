package com.wajid.videoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.File;

public class PrivatePlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String path=getIntent().getStringExtra("path");
        File file=path==null?null:new File(path);
        if(file==null || !file.exists()){finish();return;}
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);
        TextView title=new TextView(this);
        title.setText(getIntent().getStringExtra("title"));
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(12,10,12,10);
        root.addView(title);
        PlayerView view=new PlayerView(this);
        root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        player=new ExoPlayer.Builder(this).build();
        view.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
        player.prepare();
        player.play();
    }
    @Override protected void onStop(){super.onStop();if(player!=null){player.release();player=null;}}
}
