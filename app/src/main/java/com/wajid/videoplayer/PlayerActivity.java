package com.wajid.videoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private float speed = 1f;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);
        TextView title = new TextView(this);
        title.setText(getIntent().getStringExtra("title"));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setPadding(12,10,12,10);
        root.addView(title);

        PlayerView playerView = new PlayerView(this);
        playerView.setUseController(true);
        root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout controls = new LinearLayout(this);
        Button slower = new Button(this); slower.setText("− Speed");
        Button speedButton = new Button(this); speedButton.setText("1.0×");
        Button faster = new Button(this); faster.setText("+ Speed");
        slower.setOnClickListener(v -> setSpeed(Math.max(0.25f,speed-0.25f),speedButton));
        faster.setOnClickListener(v -> setSpeed(Math.min(3f,speed+0.25f),speedButton));
        speedButton.setOnClickListener(v -> setSpeed(1f,speedButton));
        controls.addView(slower,new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(speedButton,new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(faster,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(controls);
        setContentView(root);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        Uri uri = getIntent().getData();
        if (uri != null) {
            player.setMediaItem(MediaItem.fromUri(uri));
            player.prepare();
            player.play();
        }
    }

    private void setSpeed(float value, Button button) {
        speed=value;
        button.setText(String.format(java.util.Locale.US,"%.2f×",speed));
        if(player!=null) player.setPlaybackParameters(new PlaybackParameters(speed));
    }

    @Override protected void onStop() {
        super.onStop();
        if(player!=null){player.release();player=null;}
    }
}
