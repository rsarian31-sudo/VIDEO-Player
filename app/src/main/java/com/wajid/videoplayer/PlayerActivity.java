package com.wajid.videoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView titleView, positionView, durationView, seekHint, speedLabel;
    private SeekBar seekBar;
    private LinearLayout topBar, bottomPanel, speedPanel;
    private FrameLayout root;
    private float speed = 1f;
    private boolean controlsVisible = true;
    private boolean userSeeking = false;
    private float downX, downY, lastX;
    private long swipeStartPosition;
    private boolean swiping = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideControls = () -> setControlsVisible(false);
    private final Runnable progressUpdate = new Runnable() {
        @Override public void run() {
            updateProgress();
            handler.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        playerView = new PlayerView(this);
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        buildTopBar();
        buildBottomControls();
        buildSeekHint();
        setContentView(root);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int playbackState) {
                updateProgress();
            }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayButton();
            }
        });
        java.util.ArrayList<Uri> playlistUris = getIntent().getParcelableArrayListExtra("playlist_uris");
        java.util.ArrayList<String> playlistTitles = getIntent().getStringArrayListExtra("playlist_titles");
        int startIndex = getIntent().getIntExtra("playlist_index", 0);
        if (playlistUris != null && !playlistUris.isEmpty()) {
            java.util.ArrayList<MediaItem> items = new java.util.ArrayList<>();
            for (int i = 0; i < playlistUris.size(); i++) {
                MediaItem.Builder builder = new MediaItem.Builder().setUri(playlistUris.get(i));
                if (playlistTitles != null && i < playlistTitles.size()) {
                    builder.setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder().setTitle(playlistTitles.get(i)).build());
                }
                items.add(builder.build());
            }
            startIndex = Math.max(0, Math.min(startIndex, items.size() - 1));
            player.setMediaItems(items, startIndex, 0);
            player.prepare();
            player.play();
        } else {
            Uri uri = getIntent().getData();
            if (uri != null) {
                player.setMediaItem(MediaItem.fromUri(uri));
                player.prepare();
                player.play();
            }
        }
        handler.post(progressUpdate);
        scheduleHide();
    }

    private void buildTopBar() {
        topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        topBar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xD9000000, 0x00000000}));
        TextView back = textButton("‹", 38);
        back.setOnClickListener(v -> finish());
        topBar.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        titleView = new TextView(this);
        titleView.setText(getIntent().getStringExtra("title") == null ? "WAJID PLAYER" : getIntent().getStringExtra("title"));
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        topBar.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        TextView more = textButton("⋮", 28);
        more.setOnClickListener(v -> showSpeedPanel());
        topBar.addView(more, new LinearLayout.LayoutParams(dp(40), dp(48)));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(64), Gravity.TOP);
        root.addView(topBar, p);
    }

    private void buildBottomControls() {
        bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(12), dp(8), dp(12), dp(8));
        bottomPanel.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xE6000000, 0x00000000}));

        LinearLayout timeline = new LinearLayout(this);
        timeline.setGravity(Gravity.CENTER_VERTICAL);
        positionView = new TextView(this);
        positionView.setText("00:00");
        positionView.setTextColor(Color.WHITE);
        positionView.setTextSize(12);
        timeline.addView(positionView, new LinearLayout.LayoutParams(dp(52), -2));
        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF64B5F6));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF64B5F6));
        timeline.addView(seekBar, new LinearLayout.LayoutParams(0, dp(36), 1));
        durationView = new TextView(this);
        durationView.setText("−00:00");
        durationView.setTextColor(0xFFCCCCCC);
        durationView.setTextSize(12);
        durationView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        timeline.addView(durationView, new LinearLayout.LayoutParams(dp(64), -2));
        bottomPanel.addView(timeline);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        TextView lock = textButton("♙", 25);
        lock.setOnClickListener(v -> {
            topBar.setVisibility(View.GONE);
            bottomPanel.setVisibility(View.GONE);
            ToastMessage("Controls hidden. Tap video to show");
            controlsVisible = false;
        });
        buttons.addView(lock, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView previousVideo = textButton("⏮", 25);
        previousVideo.setOnClickListener(v -> { if (player != null && player.hasPreviousMediaItem()) player.seekToPreviousMediaItem(); });
        buttons.addView(previousVideo, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView play = textButton("▶", 28);
        play.setTag("play");
        play.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) player.pause(); else player.play();
            updatePlayButton();
            scheduleHide();
        });
        buttons.addView(play, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView nextVideo = textButton("⏭", 25);
        nextVideo.setOnClickListener(v -> { if (player != null && player.hasNextMediaItem()) player.seekToNextMediaItem(); });
        buttons.addView(nextVideo, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView speedButtonText = textButton("1×", 19);
        speedButtonText.setOnClickListener(v -> showSpeedPanel());
        buttons.addView(speedButtonText, new LinearLayout.LayoutParams(0, dp(56), 1));
        bottomPanel.addView(buttons);

        speedPanel = new LinearLayout(this);
        speedPanel.setGravity(Gravity.CENTER);
        speedPanel.setPadding(0, dp(4), 0, dp(4));
        Button slower = new Button(this); slower.setText("− SPEED");
        speedLabel = new TextView(this); speedLabel.setText("1.0×"); speedLabel.setTextColor(Color.WHITE); speedLabel.setGravity(Gravity.CENTER);
        Button faster = new Button(this); faster.setText("+ SPEED");
        slower.setOnClickListener(v -> setSpeed(Math.max(0.25f, speed - 0.25f)));
        faster.setOnClickListener(v -> setSpeed(Math.min(3f, speed + 0.25f)));
        speedLabel.setOnClickListener(v -> setSpeed(1f));
        speedPanel.addView(slower, new LinearLayout.LayoutParams(0, dp(44), 1));
        speedPanel.addView(speedLabel, new LinearLayout.LayoutParams(0, dp(44), 1));
        speedPanel.addView(faster, new LinearLayout.LayoutParams(0, dp(44), 1));
        speedPanel.setVisibility(View.GONE);
        bottomPanel.addView(speedPanel);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && player != null && player.getDuration() > 0) {
                    positionView.setText(formatTime(player.getDuration() * progress / 1000));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { userSeeking = true; handler.removeCallbacks(hideControls); }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (player != null && player.getDuration() > 0) player.seekTo(player.getDuration() * bar.getProgress() / 1000);
                userSeeking = false; scheduleHide();
            }
        });
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(bottomPanel, p);
    }

    private void buildSeekHint() {
        seekHint = new TextView(this);
        seekHint.setTextColor(Color.WHITE);
        seekHint.setTextSize(18);
        seekHint.setGravity(Gravity.CENTER);
        seekHint.setPadding(dp(20), dp(12), dp(20), dp(12));
        seekHint.setBackground(roundBg(0xB8000000, 14));
        seekHint.setVisibility(View.GONE);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        root.addView(seekHint, p);

        playerView.setOnTouchListener((v, event) -> handleVideoTouch(event));
    }

    private boolean handleVideoTouch(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = x; downY = y; swiping = false; swipeStartPosition = player == null ? 0 : player.getCurrentPosition();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = x - downX, dy = y - downY;
                if (!swiping && Math.abs(dx) > dp(24) && Math.abs(dx) > Math.abs(dy) * 1.3f) {
                    swiping = true; handler.removeCallbacks(hideControls);
                }
                if (swiping && player != null) {
                    long duration = player.getDuration();
                    long seekDelta = (long) ((x - downX) / Math.max(1f, playerView.getWidth()) * 120000L);
                    long target = Math.max(0, swipeStartPosition + seekDelta);
                    if (duration > 0) target = Math.min(duration, target);
                    seekHint.setText((seekDelta >= 0 ? "⏩  " : "⏪  ") + formatTime(target));
                    seekHint.setVisibility(View.VISIBLE);
                }
                lastX = x;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (swiping && player != null) {
                    long duration = player.getDuration();
                    long seekDelta = (long) ((x - downX) / Math.max(1f, playerView.getWidth()) * 120000L);
                    long target = Math.max(0, swipeStartPosition + seekDelta);
                    if (duration > 0) target = Math.min(duration, target);
                    player.seekTo(target);
                    seekHint.setVisibility(View.GONE);
                    updateProgress();
                    scheduleHide();
                } else if (Math.abs(x - downX) < dp(12) && Math.abs(y - downY) < dp(12)) {
                    setControlsVisible(!controlsVisible);
                }
                return true;
        }
        return true;
    }

    private void updateProgress() {
        if (player == null || seekBar == null) return;
        long pos = Math.max(0, player.getCurrentPosition());
        long dur = player.getDuration();
        positionView.setText(formatTime(pos));
        durationView.setText(dur > 0 ? "−" + formatTime(Math.max(0, dur - pos)) : "−00:00");
        if (!userSeeking && dur > 0) seekBar.setProgress((int) Math.min(1000, pos * 1000 / dur));
    }

    private void updatePlayButton() {
        if (bottomPanel == null) return;
        LinearLayout buttons = (LinearLayout) bottomPanel.getChildAt(1);
        TextView play = (TextView) buttons.getChildAt(2);
        play.setText(player != null && player.isPlaying() ? "Ⅱ" : "▶");
    }

    private void seekBy(long delta) {
        if (player == null) return;
        long duration = player.getDuration();
        long target = Math.max(0, player.getCurrentPosition() + delta);
        if (duration > 0) target = Math.min(duration, target);
        player.seekTo(target);
        seekHint.setText((delta > 0 ? "⏩  " : "⏪  ") + formatTime(target));
        seekHint.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> seekHint.setVisibility(View.GONE), 800);
        updateProgress(); scheduleHide();
    }

    private void setSpeed(float value) {
        speed = value;
        speedLabel.setText(String.format(Locale.US, "%.2f×", speed));
        if (player != null) player.setPlaybackParameters(new PlaybackParameters(speed));
    }

    private void showSpeedPanel() {
        speedPanel.setVisibility(speedPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        setControlsVisible(true);
        handler.removeCallbacks(hideControls);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        topBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        bottomPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) scheduleHide(); else handler.removeCallbacks(hideControls);
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideControls);
        if (player != null && player.isPlaying() && speedPanel.getVisibility() != View.VISIBLE) {
            handler.postDelayed(hideControls, 4000);
        }
    }

    private TextView textButton(String text, int size) {
        TextView b = new TextView(this);
        b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(size);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundBg(0x55000000, 28));
        return b;
    }

    private GradientDrawable roundBg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private String formatTime(long millis) {
        long total = Math.max(0, millis / 1000);
        long hours = total / 3600, minutes = (total % 3600) / 60, seconds = total % 60;
        return hours > 0 ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void ToastMessage(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override protected void onStop() {
        super.onStop();
        handler.removeCallbacks(progressUpdate);
        handler.removeCallbacks(hideControls);
        if (player != null) { player.release(); player = null; }
    }
}
