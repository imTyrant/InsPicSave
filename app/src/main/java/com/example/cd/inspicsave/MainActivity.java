package com.example.cd.inspicsave;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PicsDownloader.PicdataInteraction {
    static final int HTML_DOWNLOADEDED = 1;
    static final int PIC_DOWNLOADED = 2;
    static final int CON_TIME_OUT = 3;
    static final int PIC_SAVED = 4;
    static final int PIC_DOWNLOAD_FAILED = 5;
    static final int PIC_DECODE_FAILED = 6;
    static final int PIC_SAVE_FAILED = 7;
    static final int HTML_ANALYSIS_FAILED = 8;
    static final int INVALID_INPUT = 9;

    static final private String LOG_TAG = "URL_SHOW";
    private final String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/InsPics/";

    private Button getUrl;
    private Button save;
    private EditText inputUrl;

    private ProgressDialog detailDialog = null;

    private ImageView showImage = null;

    private TextView showHtml = null;

    private String mainhtml;
    private byte[] Image;

    private static MainActivity me;

    public static MainActivity getMainActivity(){
        return me;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        me = this;

        getUrl = (Button) findViewById(R.id.b_ok);
        save = (Button) findViewById(R.id.b_save);
        inputUrl = (EditText) findViewById(R.id.i_url);


//        showHtml = (TextView) findViewById(R.id.o_html);
        showImage = (ImageView) findViewById(R.id.showImage);

        getUrl.setOnClickListener(this);
        save.setOnClickListener(this);



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_ok:
                detailDialog = ProgressDialog.show(this, "DownLoading..","Please Wait", true, false);
                new Thread(new PicsDownloader(this)).start();
                break;
            case R.id.b_save:
                detailDialog = ProgressDialog.show(this, "Saving..","Please Wait", true, false);
                new Thread(savePicToLocal).start();
                break;
            default:
                break;
        }
    }

    private  Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case HTML_DOWNLOADEDED:
                    if(showHtml != null) {
                        showHtml.setText("");
                        showHtml.append(mainhtml);
                    }
                    break;
                case PIC_DOWNLOADED:
                    detailDialog.dismiss();
                    if (showImage != null) {
                        Bitmap bm = BitmapFactory.decodeByteArray(Image, 0, Image.length);
                        showImage.setImageBitmap(bm);
                    }
                    break;
                case PIC_SAVED:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Saving Picture Successful", Toast.LENGTH_SHORT).show();
                    break;
                case PIC_DOWNLOAD_FAILED:
                    detailDialog.dismiss();
                    Toast.makeText(me, "DownLoad Picture Failed..", Toast.LENGTH_SHORT).show();
                    break;
                case  HTML_ANALYSIS_FAILED:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Access Page Failed..", Toast.LENGTH_SHORT).show();
                    break;
                case  PIC_SAVE_FAILED:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Can't Save Picture To Local, Please Try Again", Toast.LENGTH_SHORT).show();
                    break;
                case INVALID_INPUT:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Illegal URL, Check Protocol Please!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    Runnable savePicToLocal = new Runnable() {
        @Override
        public void run() {
            if(Image == null || Image.length <= 0){
                sendMsgToMe(PIC_SAVE_FAILED);
                return;
            }
            File root = new File(savePath);
            if(!root.exists()){
                root.mkdir();
            }
            Calendar now = new GregorianCalendar();
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String picName = "INS_" + simpleDate.format(now.getTime());
            if(Image[6] == 'J' && Image[7] == 'F' && Image[8] == 'I' && Image[9] == 'F'){
                picName += ".jpg";
            }
            else if(Image[1] == 'P' && Image[2] == 'N' && Image[3] == 'G'){
                picName += ".png";
            }
            else if(Image[0] == 'G' && Image[1] == 'I' && Image[2] == 'F'){
                picName += ".gif";

            }
            else{
                sendMsgToMe(PIC_DECODE_FAILED);
                return;
            }
            try{
                File meta = new File(savePath + picName);
                FileOutputStream out = new FileOutputStream(meta);
                out.write(Image);
                out.close();
                sendMsgToMe(PIC_SAVED);
            } catch (IOException e){
                Log.e(LOG_TAG, e.getCause().toString());
                e.printStackTrace();
            }
            try{
                MediaStore.Images.Media.insertImage(me.getContentResolver(),savePath,picName,null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            me.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + savePath + picName)));
        }
    };

    public void sendMsgToMe(int state){
        handler.obtainMessage(state).sendToTarget();
    }

    @Override
    public void getPicdata(byte[] picdata) {
        Image = picdata;
    }

    @Override
    public URL shareUrl() throws MalformedURLException {
        String inputString = inputUrl.getText().toString();
        if(!inputString.substring(0,7).equals("http://") && !inputString.substring(0,8).equals("https://")){
            return null;
        }
        return new URL(inputString);
    }

    @Override
    public void test_html(String html){
        mainhtml = html;
    }
}
