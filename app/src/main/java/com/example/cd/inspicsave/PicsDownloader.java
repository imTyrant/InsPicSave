package com.example.cd.inspicsave;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by CD on 2017/6/18.
 */
public class PicsDownloader implements Runnable{

    private final String TAG_DWNLD = "PICDOWNLOAD";

    private final int TIME_OUT = 3000;

    final private String USER_AGENT = "Mozilla/5.0 (Linux; U; Android "
            + android.os.Build.VERSION.RELEASE + ";"
            + Locale.getDefault().toString() + "; " + android.os.Build.DEVICE
            + "/" + android.os.Build.ID + ")";

    private final PicdataInteraction mPicdataIA;

    private final MainActivity father;

    private URL url;

    /**
     *
     * @param initedMethods
     */
    public PicsDownloader(PicdataInteraction initedMethods){

        this.mPicdataIA = initedMethods;

        this.father = MainActivity.getMainActivity();

    }

    /**
     *
     * @param inStream
     * @return
     * @throws IOException
     *
     * Read from InputStream
     */
    public static byte[] readInputStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        return outStream.toByteArray();
    }

    /**
     * @return
     * Download html from target html page url.
     */
    private String getHtmlOfImage(){

        try {
            String htmlCharset = null;

            URLConnection urlConnection = url.openConnection();

            urlConnection.setConnectTimeout(TIME_OUT);

            String rawTypeString = urlConnection.getContentType();


            if (rawTypeString == null) {
                htmlCharset = "utf-8";
            } else {
                String[] ContentInfos = rawTypeString.split("=");
                if (ContentInfos.length < 2) {
                    htmlCharset = "utf-8";
                } else {
                    htmlCharset = ContentInfos[1];
                }
            }

            InputStream inStream = urlConnection.getInputStream();

            byte[] readData = readInputStream(inStream);

            if (readData == null || readData.length < 1){
                return null;
            }

            return new String(readData, htmlCharset);

        }catch (IOException e){
            return null;
        }
    }

//    private String findPicScr(String html){
//        //#pImage_11
//        ////*[@id="pImage_12"]
//        Document doc = Jsoup.parse(html);
//        Elements elem = doc.select("meta[property = og:image]");
//        return elem.attr("content");
//    }

    private List<String> GetAllPicURL(String html){
        try {

            InsHtmlAnalisis insHA = new InsHtmlAnalisis(html);

            return new ArrayList<>(insHA.getAllImageURL());

        }  catch (Exception e) {

            if (e instanceof AnalysisException || e instanceof JSONException){
                Document doc = Jsoup.parse(html);
                Elements elem = doc.select("meta[property = og:image]");
                List<String> rtn = new ArrayList<>();
                rtn.add(elem.attr("content"));
                return rtn;
            }

            else{
                return null;
            }
        }
    }


    private byte[] downloadImage(String imageUrl){
        byte[] readData;
        URL imageURL;
        try {
            if(!imageUrl.substring(0,7).equals("http://") && !imageUrl.substring(0,8).equals("https://")){
                String buffer = imageUrl;
                imageUrl = url.getProtocol() + "://" + buffer;
            }
            imageURL = new URL(imageUrl);

            URLConnection uc = imageURL.openConnection();

            uc.setConnectTimeout(TIME_OUT);

            InputStream inStream = uc.getInputStream();

            readData = readInputStream(inStream);

            return readData;

        } catch (MalformedURLException e){
            father.sendMsgToMe(MainActivity.PIC_DOWNLOAD_FAILED);
            e.printStackTrace();
            return null;

        } catch (IOException e) {
            father.sendMsgToMe(MainActivity.PIC_DOWNLOAD_FAILED);
            e.printStackTrace();
            return null;
        }
    }

    public URL analyURL(String iURL){
        URL rtnURL;
        try {
            rtnURL = new URL(iURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        if (!rtnURL.getProtocol().equals("https")){
            return null;
        }
        if (!rtnURL.getHost().equals("www.instagram.com")){
            return null;
        }
        return rtnURL;
    }

    @Override
    public void run(){
        try {

            if(null == (url = analyURL(mPicdataIA.setTargetURL()))){
                father.sendMsgToMe(MainActivity.INVALID_INPUT);
                return;
            }

            String html = getHtmlOfImage();

            if (html == null) {
                father.sendMsgToMe(MainActivity.HTML_ANALYSIS_FAILED);
                return;
            }

            /**
            father.sendMsgToMe(MainActivity.HTML_DOWNLOADEDED);
            **/

            List<String> PicURLs = GetAllPicURL(html);

            if (PicURLs == null){
                father.sendMsgToMe(MainActivity.HTML_ANALYSIS_FAILED);
                return;
            }

            List<Bitmap> PicsData = new ArrayList<>();

            for (String picURL : PicURLs){
                byte[] downloadedData = downloadImage(picURL);

                if(downloadedData == null){
                    father.sendMsgToMe(MainActivity.PIC_DOWNLOAD_FAILED);
                    return;
                }
                PicsData.add(BitmapFactory.decodeByteArray(downloadedData, 0, downloadedData.length));
            }

            father.getPicsData(PicsData);

//            father.getPicdata(downloadedData);

            father.sendMsgToMe(MainActivity.PIC_DOWNLOADED);

        } catch (Exception e) {
            father.sendMsgToMe(MainActivity.UNKNOW_ERROR);
        }
    }

    interface PicdataInteraction{
        String setTargetURL();
        void getPicdata(byte[] picdata);
        void getPicsData(List<Bitmap> picsData);
        void test_html(String html);
    }
}
