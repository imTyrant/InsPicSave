package com.example.cd.inspicsave;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by CD on 2017/7/2.
 */

public class InsHtmlAnalisis {

    private String Html;

    public InsHtmlAnalisis(String Html) throws AnalysisException, JSONException {
        this.Html = Html;
    }

    public List<String> getAllImageURL() throws AnalysisException, JSONException {

        List<String> rtnArry = new ArrayList<String>();

        String rawJson = "";

        Document doc = Jsoup.parse(Html);

        Elements scripts = doc.select("script");
//        System.out.println(scripts.last());
        for(Element e : scripts){
            if(e.toString().length() > 10000){
//                String[] afterSplit = e.toString().substring(0,60).split(">");
//                if(afterSplit[1].substring(0,18).equals("window._sharedData")){
//                    rawData = e.toString().substring(afterSplit[0].length() + 1 + "windows.".length());
//                    System.out.println(rawData);
//                    break;
//                }
                String[] afterSplit = e.toString().substring(0,60).split("\\{");
                rawJson = e.toString().substring( afterSplit[0].length() , (e.toString().length() - 10));
//                System.out.println(rawJson);
            }
        }

        JSONObject jb = new JSONObject(rawJson);
        if (null == jb){
            throw new AnalysisException();
        }

        JSONObject entry_data;
        if (null == (entry_data = jb.getJSONObject("entry_data"))){
            throw new AnalysisException();
        }

        JSONArray PostPage;
        if(null == (PostPage = entry_data.getJSONArray("PostPage"))){
            throw new AnalysisException();
        }


        if (PostPage.isNull(0)){
            throw new AnalysisException();
        }

        JSONObject graphql;
        if (null == (graphql = PostPage.getJSONObject(0).getJSONObject("graphql"))){
            throw new AnalysisException();
        }

        JSONObject shortcode_media;

        if (null == (shortcode_media = graphql.getJSONObject("shortcode_media"))){
            throw new AnalysisException();
        }

        String type;

        if (null == (type = shortcode_media.getString("__typename"))){
            throw new AnalysisException();
        }

        System.out.println(type);

        if (type.equals("GraphSidecar")){

            JSONArray edges;

            if(shortcode_media.isNull("edge_sidecar_to_children")){
                throw new AnalysisException();
            }

            if(null == (edges = shortcode_media.getJSONObject("edge_sidecar_to_children").getJSONArray("edges"))){
                throw new AnalysisException();
            }

            for (int i = 0; i < edges.length(); i++){

                try {
                    rtnArry.add(edges.getJSONObject(i).getJSONObject("node").getString("display_url"));
                } catch (Exception e){
                    throw new AnalysisException();
                }

            }
            return rtnArry;
        }

        Elements singleURL = doc.select("meta[property = og:image]");
        rtnArry.add(singleURL.attr("content"));
        return rtnArry;
    }

}
