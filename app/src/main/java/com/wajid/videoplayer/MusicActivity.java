package com.wajid.videoplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MusicActivity extends AppCompatActivity {
    private static final int BG=0xFF101115, PANEL=0xFF1D1F26, MUTED=0xFFA4A8B4, ACCENT=0xFF4191FF;
    private LinearLayout list;
    private TextView count;
    private String folderFilter=null;
    private final ArrayList<Song> songs=new ArrayList<>();
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private static class Song { Uri uri; String name,folder; long duration; Song(Uri u,String n,String f,long d){uri=u;name=n;folder=f;duration=d;} }
    private final ActivityResultLauncher<String> permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{if(granted)loadSongs();else message("Allow music/audio access to show MP3 files.");});

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(12),dp(16),0); root.setBackgroundColor(BG);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.statusBars()|WindowInsetsCompat.Type.navigationBars());view.setPadding(dp(16),bars.top+dp(12),dp(16),bars.bottom);return insets;});
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=text("‹",34,Color.WHITE,false); back.setPadding(0,0,dp(16),0); back.setOnClickListener(v->finish()); top.addView(back);
        LinearLayout titleBox=new LinearLayout(this); titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text("WAJID MUSIC",22,Color.WHITE,true)); titleBox.addView(text("MP3 • AUDIO LIBRARY",10,MUTED,true)); top.addView(titleBox);
        root.addView(top);
        count=text("Loading songs…",12,MUTED,false); count.setPadding(0,dp(18),0,dp(10)); root.addView(count);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView note=text("Music keeps playing when you leave this screen or turn off the display.",11,MUTED,false); note.setGravity(Gravity.CENTER); note.setPadding(0,dp(8),0,dp(12)); root.addView(note);
        setContentView(root);
        String permission=Build.VERSION.SDK_INT>=33? "android.permission.READ_MEDIA_AUDIO":Manifest.permission.READ_EXTERNAL_STORAGE;
        if(ContextCompat.checkSelfPermission(this,permission)==PackageManager.PERMISSION_GRANTED)loadSongs();else permissionLauncher.launch(permission);
    }
    private void connectAndPlay(ArrayList<Song> playlist,int index){
        SessionToken token=new SessionToken(this,new ComponentName(this,MusicPlaybackService.class));
        controllerFuture=new MediaController.Builder(this,token).buildAsync();
        controllerFuture.addListener(()->{
            try{
                controller=controllerFuture.get();
                ArrayList<MediaItem> items=new ArrayList<>();
                for(Song s:playlist) items.add(new MediaItem.Builder().setUri(s.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(s.name).setArtist("WAJID MUSIC").build()).build());
                controller.setMediaItems(items,Math.max(0,Math.min(index,items.size()-1)),0);
                controller.prepare(); controller.play();
                android.widget.Toast.makeText(this,"Playing: "+playlist.get(index).name,android.widget.Toast.LENGTH_SHORT).show();
            }catch(Exception e){message("Could not start audio playback.");}
        },ContextCompat.getMainExecutor(this));
    }
    private void loadSongs(){
        songs.clear(); Uri collection=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.BUCKET_DISPLAY_NAME};
        try(Cursor c=getContentResolver().query(collection,projection,MediaStore.Audio.Media.IS_MUSIC+" != 0",null,MediaStore.Audio.Media.TITLE+" COLLATE NOCASE ASC")){
            if(c!=null)while(c.moveToNext()){long id=c.getLong(0);String name=c.getString(1);long duration=c.getLong(2);String folder=c.getString(3);if(folder==null||folder.trim().isEmpty())folder="Music";songs.add(new Song(Uri.withAppendedPath(collection,String.valueOf(id)),name==null?"Unknown audio":name,folder,duration));}
        }catch(Exception ignored){}
        render();
    }
    private void render(){
        list.removeAllViews();
        if(folderFilter==null){
            Map<String,ArrayList<Song>> folders=new LinkedHashMap<>(); for(Song s:songs)folders.computeIfAbsent(s.folder,k->new ArrayList<>()).add(s);
            count.setText(songs.size()+" audio files • "+folders.size()+" folders");
            if(songs.isEmpty()){message("No music files found. Put MP3 files in a folder such as Music, then reopen this screen.");return;}
            for(Map.Entry<String,ArrayList<Song>> e:folders.entrySet()){
                LinearLayout row=row(); row.addView(text("♫",30,ACCENT,true),new LinearLayout.LayoutParams(dp(52),dp(52)));
                LinearLayout details=column(); details.addView(text(e.getKey(),16,Color.WHITE,true)); details.addView(text(e.getValue().size()+" songs",12,MUTED,false)); row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
                row.addView(text("›",28,MUTED,false)); row.setOnClickListener(v->{folderFilter=e.getKey();render();}); addRow(row);
            }
        }else{
            ArrayList<Song> filtered=new ArrayList<>();for(Song s:songs)if(folderFilter.equals(s.folder))filtered.add(s);
            count.setText(filtered.size()+" songs");
            TextView back=text("‹  All music folders",14,ACCENT,true);back.setPadding(0,dp(8),0,dp(12));back.setOnClickListener(v->{folderFilter=null;render();});list.addView(back);
            for(int i=0;i<filtered.size();i++){final int index=i;final ArrayList<Song> playlist=new ArrayList<>(filtered);Song s=filtered.get(i);
                LinearLayout row=row();row.addView(text("♫",24,ACCENT,true),new LinearLayout.LayoutParams(dp(44),dp(44)));
                LinearLayout details=column();TextView name=text(s.name,14,Color.WHITE,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);details.addView(name);details.addView(text(time(s.duration),11,MUTED,false));row.addView(details,new LinearLayout.LayoutParams(0,-2,1));row.addView(text("▶",18,ACCENT,true));row.setOnClickListener(v->connectAndPlay(playlist,index));addRow(row);
            }
        }
    }
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(12),dp(10),dp(12),dp(10));r.setBackground(round(PANEL,dp(12)));return r;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(12),0,dp(8),0);return l;}
    private void addRow(View v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);list.addView(v,p);}
    private TextView text(String value,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private String time(long ms){long sec=ms/1000;return String.format(Locale.US,"%02d:%02d",sec/60,sec%60);}
    private void message(String s){if(list!=null){list.removeAllViews();TextView t=text(s,14,MUTED,false);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(24),dp(12),dp(24));list.addView(t);}}
    @Override protected void onDestroy(){super.onDestroy();if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture);}
}
