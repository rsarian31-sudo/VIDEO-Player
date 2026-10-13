package com.wajid.videoplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private LinearLayout list;
    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) loadVideos(); else showMessage("Allow video access to display videos on your phone.");
        });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,12);
        root.setBackgroundColor(0xFF101014);

        TextView title = new TextView(this);
        title.setText("WAJID PLAYER");
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(null,1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Your personal video library");
        subtitle.setTextColor(0xFFBDBDC7);
        root.addView(subtitle);

        LinearLayout actions = new LinearLayout(this);
        Button refresh = makeButton("Refresh");
        refresh.setOnClickListener(v -> checkPermission());
        Button privateButton = makeButton("🔒 Private Folder");
        privateButton.setOnClickListener(v -> startActivity(new Intent(this, PrivateActivity.class)));
        actions.addView(refresh,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(privateButton,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions);

        TextView heading = new TextView(this);
        heading.setText("ALL VIDEOS");
        heading.setTextColor(0xFFB69CFF);
        heading.setPadding(0,18,0,8);
        root.addView(heading);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        checkPermission();
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6441A5));
        return b;
    }

    private void checkPermission() {
        String permission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) loadVideos();
        else permissionLauncher.launch(permission);
    }

    private void showMessage(String message) {
        list.removeAllViews();
        TextView t = new TextView(this);
        t.setText(message);
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(4,20,4,20);
        list.addView(t);
    }

    private void loadVideos() {
        list.removeAllViews();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION};
        int count = 0;
        try (Cursor c = getContentResolver().query(collection, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (c != null) while (c.moveToNext()) {
                long id = c.getLong(0);
                String name = c.getString(1);
                long ms = c.getLong(2);
                Uri uri = ContentUris.withAppendedId(collection,id);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(12,12,12,12);
                TextView n = new TextView(this);
                n.setText("▶  " + name);
                n.setTextColor(0xFFFFFFFF);
                n.setTextSize(16);
                TextView d = new TextView(this);
                d.setText(formatDuration(ms));
                d.setTextColor(0xFFAAAAB8);
                row.addView(n); row.addView(d);
                row.setOnClickListener(v -> {
                    Intent i = new Intent(this, PlayerActivity.class);
                    i.setData(uri); i.putExtra("title",name); startActivity(i);
                });
                list.addView(row);
                View divider = new View(this);
                divider.setBackgroundColor(0xFF292832);
                list.addView(divider,new LinearLayout.LayoutParams(-1,1));
                count++;
            }
        } catch (Exception ignored) { }
        if (count == 0) showMessage("No videos found yet.");
    }

    private String formatDuration(long ms) {
        long sec=ms/1000, h=sec/3600, m=(sec%3600)/60, s=sec%60;
        return h>0 ? String.format("%d:%02d:%02d",h,m,s) : String.format("%d:%02d",m,s);
    }
}
