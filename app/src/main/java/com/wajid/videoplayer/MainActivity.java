package com.wajid.videoplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int BG = Color.rgb(16,17,21);
    private static final int PANEL = Color.rgb(29,31,38);
    private static final int MUTED = Color.rgb(164,168,180);
    private static final int ACCENT = Color.rgb(65,145,255);
    private LinearLayout list;
    private EditText search;
    private TextView countLabel;
    private final ArrayList<VideoItem> videos = new ArrayList<>();
    private String folderFilter = null;

    private static class VideoItem {
        Uri uri; String name; String duration; String folder;
        VideoItem(Uri u, String n, String d, String f) { uri=u; name=n; duration=d; folder=f; }
    }

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) loadVideos(); else showMessage("Allow video access to show videos stored on your phone.");
        });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildLayout();
        checkPermission();
    }

    @Override public void onBackPressed() {
        if (folderFilter != null) {
            folderFilter = null;
            if (search != null) search.setText("");
            renderList();
            return;
        }
        super.onBackPressed();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),0);
        root.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("WAJID PLAYER",22,Color.WHITE,true);
        TextView subtitle = text("VIDEO PLAYER",10,MUTED,true);
        subtitle.setLetterSpacing(0.16f);
        brand.addView(title); brand.addView(subtitle);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        Button updateBtn = makeButton("↻ Update");
        updateBtn.setTextSize(11);
        updateBtn.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("WAJID PLAYER Update")
                .setMessage("Open the official update page to get the latest version. Android will ask you to confirm installation.")
                .setNegativeButton("Cancel", (d,w) -> d.dismiss())
                .setPositiveButton("Check updates", (d,w) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rsarian31-sudo/VIDEO-Player/releases/tag/mobile-latest"));
                    startActivity(i);
                }).show();
        });
        actions.addView(updateBtn);
        Button privateBtn = makeButton("🔒 Private");
        privateBtn.setTextSize(11);
        privateBtn.setOnClickListener(v -> startActivity(new Intent(this,PrivateActivity.class)));
        actions.addView(privateBtn);
        top.addView(actions);
        root.addView(top);

        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(12),0,dp(10),0);
        searchBox.setBackground(round(PANEL,dp(12)));
        TextView magnify = text("⌕",26,MUTED,false);
        searchBox.addView(magnify);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("Search folders and videos");
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(8),dp(10),dp(4),dp(10));
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { renderList(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        searchBox.addView(search,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(searchBox,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams sp = (LinearLayout.LayoutParams)searchBox.getLayoutParams();
        sp.topMargin=dp(16); sp.bottomMargin=dp(14); searchBox.setLayoutParams(sp);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(0,dp(15),0,dp(8));
        TextView section = text("YOUR LIBRARY",11,MUTED,true);
        section.setLetterSpacing(0.12f);
        heading.addView(section,new LinearLayout.LayoutParams(0,-2,1));
        countLabel = text("0 videos",11,MUTED,false);
        heading.addView(countLabel);
        root.addView(heading);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer = text("Local videos • Private folder",10,MUTED,false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,dp(8),0,dp(12));
        root.addView(footer);
        setContentView(root);
    }

    private TextView text(String value,float size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private Button makeButton(String label) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF263B56));
        return b;
    }

    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    private void checkPermission() {
        String p=Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_VIDEO:Manifest.permission.READ_EXTERNAL_STORAGE;
        if(ContextCompat.checkSelfPermission(this,p)==PackageManager.PERMISSION_GRANTED)loadVideos();
        else permissionLauncher.launch(p);
    }

    private void loadVideos() {
        videos.clear();
        Uri collection=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.BUCKET_DISPLAY_NAME};
        try(Cursor c=getContentResolver().query(collection,projection,null,null,MediaStore.Video.Media.DATE_ADDED+" DESC")) {
            if(c!=null)while(c.moveToNext()) {
                long id=c.getLong(0); String name=c.getString(1); long ms=c.getLong(2);
                String folder=c.getString(3); if(folder==null||folder.trim().isEmpty())folder="Other";
                videos.add(new VideoItem(ContentUris.withAppendedId(collection,id),name,formatDuration(ms),folder));
            }
        } catch(Exception ignored) {}
        renderList();
    }

    private void renderList() {
        if(list==null)return;
        list.removeAllViews();
        String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);

        // Main screen shows folders only. Opening a folder shows the videos inside it.
        if(folderFilter==null) {
            Map<String,ArrayList<VideoItem>> folders=new LinkedHashMap<>();
            for(VideoItem item:videos) if(item.folder.toLowerCase(Locale.ROOT).contains(q))
                folders.computeIfAbsent(item.folder,k->new ArrayList<>()).add(item);
            countLabel.setText(folders.size()+" folders");
            if(folders.isEmpty()){showMessage("No folders found");return;}
            for(Map.Entry<String,ArrayList<VideoItem>> e:folders.entrySet()) {
                LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12),dp(12),dp(12),dp(12));
                row.setBackground(round(PANEL,dp(12)));
                TextView icon=text("▰",30,ACCENT,true); icon.setGravity(Gravity.CENTER); row.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));
                LinearLayout details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(12),0,0,0);
                details.addView(text(e.getKey(),15,Color.WHITE,true));
                details.addView(text(e.getValue().size()+" videos",12,MUTED,false));
                row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
                TextView arrow=text("›",28,MUTED,false); row.addView(arrow);
                row.setOnClickListener(v->{folderFilter=e.getKey();renderList();});
                addSpacedRow(row);
            }
            return;
        }

        ArrayList<VideoItem> filtered=new ArrayList<>();
        for(VideoItem item:videos) {
            boolean matches=item.name.toLowerCase(Locale.ROOT).contains(q);
            if(matches&&folderFilter.equals(item.folder))filtered.add(item);
        }
        countLabel.setText(filtered.size()+" videos");
        TextView back=text("‹  All folders     "+folderFilter,13,ACCENT,true);
        back.setPadding(dp(4),dp(8),0,dp(12));
        back.setOnClickListener(v->{folderFilter=null;renderList();});
        list.addView(back);
        if(filtered.isEmpty()){showMessage("No videos found");return;}
        for(VideoItem item:filtered) {
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(8),dp(8),dp(8),dp(8));
            row.setBackground(round(PANEL,dp(12)));
            ImageView thumb=new ImageView(this); thumb.setScaleType(ImageView.ScaleType.CENTER_CROP); thumb.setBackground(round(0xFF343844,dp(8)));
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(dp(104),dp(68)); tp.rightMargin=dp(12);
            try { if(Build.VERSION.SDK_INT>=29) { Bitmap bmp=getContentResolver().loadThumbnail(item.uri,new Size(dp(104),dp(68)),null); thumb.setImageBitmap(bmp); } }
            catch(Exception ignored) {}
            if(thumb.getDrawable()==null) { thumb.setImageDrawable(null); thumb.setBackground(round(0xFF343844,dp(8))); }
            row.addView(thumb,tp);
            LinearLayout details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL);
            TextView name=text(item.name,14,Color.WHITE,true); name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(name);
            TextView meta=text(item.folder+"  •  "+item.duration,11,MUTED,false); meta.setPadding(0,dp(5),0,0); details.addView(meta);
            row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView more=text("⋮",24,MUTED,true); more.setPadding(dp(8),0,dp(2),0); row.addView(more);
            row.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.setData(item.uri);i.putExtra("title",item.name);
                ArrayList<Uri> playlistUris=new ArrayList<>(); ArrayList<String> playlistTitles=new ArrayList<>(); int startIndex=0;
                for(int idx=0;idx<filtered.size();idx++){VideoItem video=filtered.get(idx);playlistUris.add(video.uri);playlistTitles.add(video.name);if(video.uri.equals(item.uri))startIndex=idx;}
                i.putParcelableArrayListExtra("playlist_uris",playlistUris);i.putStringArrayListExtra("playlist_titles",playlistTitles);i.putExtra("playlist_index",startIndex);startActivity(i);});
            addSpacedRow(row);
        }
    }

    private void addSpacedRow(View row) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); list.addView(row,p);
    }

    private void showMessage(String message) {
        TextView t=text(message,14,MUTED,false); t.setGravity(Gravity.CENTER); t.setPadding(0,dp(30),0,dp(30)); list.addView(t);
    }

    private String formatDuration(long ms) {
        long sec=ms/1000,h=sec/3600,m=(sec%3600)/60,s=sec%60;
        return h>0?String.format(Locale.US,"%d:%02d:%02d",h,m,s):String.format(Locale.US,"%d:%02d",m,s);
    }
}
