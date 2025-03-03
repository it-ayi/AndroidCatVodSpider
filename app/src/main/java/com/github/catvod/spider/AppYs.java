package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppYs extends Spider {
    private static String siteUrl = "http://yun.itayi.xyz"; // 替换为实际的 API 地址

    private Map<String, String> getHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        return header;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        if (!extend.isEmpty()) {
            siteUrl = extend; // 允许传入新的 API 地址
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        String url = siteUrl + "/homeContent?filter=" + filter; // 构建请求 URL
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));
        
        List<Class> classes = new ArrayList<>();
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        List<Vod> list = new ArrayList<>();

        // 处理类别
        JSONArray classArray = jsonResponse.getJSONArray("class");
        for (int i = 0; i < classArray.length(); i++) {
            JSONObject classObj = classArray.getJSONObject(i);
            classes.add(new Class(classObj.getString("type_id"), classObj.getString("type_name")));
        }

// 处理过滤器
JSONObject filtersObj = jsonResponse.getJSONObject("filters");
for (Iterator<String> it = filtersObj.keys(); it.hasNext();) {
    String key = it.next();
    JSONArray filterArray = filtersObj.getJSONArray(key);
    List<Filter> filterList = new ArrayList<>();
    for (int j = 0; j < filterArray.length(); j++) {
        JSONObject filterObj = filterArray.getJSONObject(j);
        List<Filter.Value> values = new ArrayList<>();
        JSONArray valueArray = filterObj.getJSONArray("value");
        for (int k = 0; k < valueArray.length(); k++) {
            JSONObject valueObj = valueArray.getJSONObject(k);
            values.add(new Filter.Value(valueObj.getString("n"), valueObj.getString("v")));
        }
        filterList.add(new Filter(filterObj.getString("key"), filterObj.getString("name"), values));
    }
    filters.put(key, filterList);
}

        // 处理视频列表
        JSONArray vodArray = jsonResponse.getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            ));
        }

        return Result.string(classes, list, filters); // 返回结果
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String url = siteUrl + String.format("/categoryContent?tid=%s&pg=%s&filter=%s", tid, pg, filter);
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonResponse.getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            ));
        }

        return Result.string(list); // 返回视频列表
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String url = siteUrl + String.format("/detailContent?ids=%s", new JSONArray(ids).toString());
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));
        
        JSONArray vodArray = jsonResponse.getJSONArray("list");
        if (vodArray.length() > 0) {
            JSONObject vodObj = vodArray.getJSONObject(0);
            Vod vod = new Vod();
            vod.setVodId(vodObj.getString("vod_id"));
            vod.setVodName(vodObj.getString("vod_name"));
            vod.setVodPic(vodObj.getString("vod_pic"));
            vod.setVodYear(vodObj.getString("vod_year"));
            vod.setVodArea(vodObj.getString("vod_area"));
            vod.setVodRemarks(vodObj.getString("vod_remarks"));
            vod.setVodActor(vodObj.getString("vod_actor"));
            vod.setVodDirector(vodObj.getString("vod_director"));
            vod.setVodContent(vodObj.getString("vod_content"));
            vod.setVodPlayFrom(vodObj.getString("vod_play_from"));
            vod.setVodPlayUrl(vodObj.getString("vod_play_url"));

            return Result.string(vod); // 返回视频详情
        }

        return Result.error("视频内容未找到"); // 返回错误信息
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String url = siteUrl + String.format("/searchContent?key=%s&quick=%s", key, quick);
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonResponse.getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            ));
        }

        return Result.string(list); // 返回搜索结果
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String url = siteUrl + String.format("/playerContent?id=%s&flag=%s&vipFlags=%s", id, flag, new JSONArray(vipFlags).toString());
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));
        
        String videoUrl = jsonResponse.getString("url");
        return Result.get().url(videoUrl).header(getHeader()).string(); // 返回播放地址
    }
    }
