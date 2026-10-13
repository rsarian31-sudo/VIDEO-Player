package com.wajid.videoplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

public class PrivateActivity extends AppCompatActivity {
    private static final String PREF="private_settings", HASH="pin_hash";
    private LinearLayout list;
    private final ActivityResultLauncher<String> picker =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> { if(uri!=null) importVideo(uri); });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String saved = getSharedPreferences(PREF,0).getString(HASH,null);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint(saved==null ? "Create a 4–8 digit PIN" : "Enter PIN");
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(saved==null ? "Set Private Folder PIN" : "Unlock Private Folder")
            .setMessage(saved==null ? "Choose a PIN you can remember." : "Enter your PIN to view private videos.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(saved==null ? "Set PIN" : "Unlock",null)
            .setNegativeButton("Cancel",(d,w)->finish())
            .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String pin=input.getText().toString();
            if(pin.length()<4 || pin.length()>8) { input.setError("PIN must be 4–8 digits"); return; }
            String hash=hash(pin);
            if(saved==null) {
                getSharedPreferences(PREF,0).edit().putString(HASH,hash).apply();
                dialog.dismiss(); showPage();
            } else if(saved.equals(hash)) {
                dialog.dismiss(); showPage();
            } else input.setError("Incorrect PIN");
        }));
        dialog.show();
    }

    private void showPage() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,18);
        root.setBackgroundColor(0xFF101014);
        TextView title=new TextView(this);
        title.setText("🔒 PRIVATE FOLDER");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(22);
        root.addView(title);
        TextView note=new TextView(this);
        note.setText("Imported videos are copied into this app's private storage.");
        note.setTextColor(0xFFBBBBCC);
        note.setPadding(0,8,0,12);
        root.addView(note);
        Button add=new Button(this);
        add.setText("＋ Import video");
        add.setOnClickListener(v -> picker.launch("video/*"));
        root.addView(add);
        list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        refreshList();
    }

    private void importVideo(Uri uri) {
        try {
            File dir=new File(getFilesDir(),"private_videos"); dir.mkdirs();
            String type=getContentResolver().getType(uri);
            String ext=(type!=null && type.contains("webm")) ? ".webm" : ".mp4";
            File target=new File(dir,"private_"+System.currentTimeMillis()+ext);
            try(InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(target)) {
                if(in==null) throw new Exception("Cannot open selected video");
                byte[] buffer=new byte[8192]; int n;
                while((n=in.read(buffer))>0) out.write(buffer,0,n);
            }
            Toast.makeText(this,"Video added to Private Folder",Toast.LENGTH_LONG).show();
            refreshList();
        } catch(Exception e) { Toast.makeText(this,"Import failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    private void refreshList() {
        if(list==null)return;
        list.removeAllViews();
        File dir=new File(getFilesDir(),"private_videos"); dir.mkdirs();
        File[] files=dir.listFiles();
        if(files==null || files.length==0) {
            TextView empty=new TextView(this); empty.setText("No private videos yet.");
            empty.setTextColor(0xFFCCCCD5); empty.setPadding(0,16,0,16); list.addView(empty); return;
        }
        for(File f:files) {
            Button item=new Button(this); item.setText("▶  "+f.getName()); item.setAllCaps(false);
            item.setOnClickListener(v -> {
                Intent i=new Intent(this,PrivatePlayerActivity.class);
                i.putExtra("path",f.getAbsolutePath()); i.putExtra("title",f.getName()); startActivity(i);
            });
            list.addView(item);
        }
    }

    private String hash(String pin) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(pin.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder();
            for(byte b:bytes) out.append(String.format(Locale.US,"%02x",b));
            return out.toString();
        } catch(Exception e) { return pin; }
    }
}
