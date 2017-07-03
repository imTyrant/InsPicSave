package com.example.cd.inspicsave;

import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnClickListener, PicsDownloader.PicdataInteraction {
    static final int UNKNOW_ERROR = 255;
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
    private final String cachePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/InsPics/cache/";

    private Button getUrl;

    private EditText inputUrl;

    private ProgressDialog detailDialog = null;

    private ImageView showImage = null;

    private List<ImageView> showImages = null;

    private ViewPager viewPager = null;

    private TextView showHtml = null;
    /*********************************************************/
    private int currentPage = 0;

    private int pageToBeDownload = 0;

    private int totalImageNum = 0;

    private String mainhtml;

    private List<Bitmap> Images;

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
        inputUrl = (EditText) findViewById(R.id.i_url);

//        showHtml = (TextView) findViewById(R.id.o_html);
//        showImage = (ImageView) findViewById(R.id.showImage);
        viewPager = (ViewPager)findViewById(R.id.showImages);

        getUrl.setOnClickListener(this);

//        viewPager.setOnLongClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_ok:
                detailDialog = ProgressDialog.show(this, "DownLoading..","Please Wait", true, false);
                new Thread(new PicsDownloader(this)).start();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.showImages:
                currentPage = viewPager.getCurrentItem();
                Toast.makeText(me, currentPage, Toast.LENGTH_SHORT).show();
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                });
//                builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                });

                return true;
            default:
                return false;
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
                    viewPager.setAdapter(new PicPageAdapter());
                    viewPager.addOnPageChangeListener(new PicPagerListener());
                    detailDialog.dismiss();
//                    if (showImage != null) {
//                        Bitmap bm = BitmapFactory.decodeByteArray(Images.get(cur), 0, Images.get(cur).length);
//                        showImage.setImageBitmap(bm);
//                    }
                    break;
                case PIC_DOWNLOAD_FAILED:
                    detailDialog.dismiss();
                    Toast.makeText(me, "DownLoad Picture Failed..", Toast.LENGTH_SHORT).show();
                    break;
                case  HTML_ANALYSIS_FAILED:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Access Page Failed..", Toast.LENGTH_SHORT).show();
                    break;

                case INVALID_INPUT:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Illegal URL! Check Your URL!", Toast.LENGTH_SHORT).show();
                    break;
                case PIC_SAVED:
                    Toast.makeText(me, "Saving Picture Successful", Toast.LENGTH_SHORT).show();
                    break;
                case  PIC_SAVE_FAILED:
                    Toast.makeText(me, "Can't Save Picture To Local, Please Try Again", Toast.LENGTH_SHORT).show();
                    break;
                case UNKNOW_ERROR:
                    detailDialog.dismiss();
                    Toast.makeText(me, "Oops! unknown error happened! Try again, or contact with us.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    Runnable savePicToLocal = new Runnable() {

        @Override
        public void run() {

            if(Images == null || Images.size() < pageToBeDownload){
                sendMsgToMe(PIC_SAVE_FAILED);
                return;
            }
            File root = new File(savePath);
            if(!root.exists()){
                root.mkdir();
            }
            Calendar now = new GregorianCalendar();
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String picName = "INS_" + simpleDate.format(now.getTime()) + ".png";
//            if(Image[6] == 'J' && Image[7] == 'F' && Image[8] == 'I' && Image[9] == 'F'){
//                picName += ".jpg";
//            }
//            else if(Image[1] == 'P' && Image[2] == 'N' && Image[3] == 'G'){
//                picName += ".png";
//            }
//            else if(Image[0] == 'G' && Image[1] == 'I' && Image[2] == 'F'){
//                picName += ".gif";
//
//            }
//            else{
//                sendMsgToMe(PIC_DECODE_FAILED);
//                return;
//            }
            try{
                File meta = new File(savePath + picName);
                FileOutputStream out = new FileOutputStream(meta);
                Images.get(pageToBeDownload).compress(Bitmap.CompressFormat.PNG, 100, out);
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
    /**
     * Get images data from thread. Container is a list.
     */
    @Override
    public void getPicsData(List<Bitmap> picsData){
        Images = picsData;
        totalImageNum = picsData.size();
        showImages = new ArrayList<>();
        for (Bitmap bm : Images){
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bm);
            showImages.add(imageView);
        }
    }
    /**
     * Get image data from thread.
     * NOT USED!
     */
    @Override
    public void getPicdata(byte[] picdata) {
//        Image = picdata;
    }
    /**
     * Set target html page url, return String.
     */
    @Override
    public String setTargetURL(){
        return inputUrl.getText().toString();
    }
    /**
     * Test method. To show html page downloaded.
     * NOT USED!
     */

    @Override
    public void test_html(String html){
        mainhtml = html;
    }

    private class PicPagerListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    /**
     * ViewPager class implement.
     */
    private class PicPageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return totalImageNum;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem (ViewGroup container, int position, Object object) {
            container.removeView(showImages.get(position));
        }

        @Override
        public Object instantiateItem (ViewGroup container, final int position) {

            ImageView cur = showImages.get(position);

            cur.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    pageToBeDownload = viewPager.getCurrentItem();
                    AlertDialog.Builder builder = new AlertDialog.Builder(me);
                    builder.setMessage("Save This Picture To Local?")
                            .setCancelable(true)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    new Thread(savePicToLocal).start();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).show();
                    builder.create();
                    return true;
                }
            });
            container.addView(cur);
            return cur;
        }

    }
}
