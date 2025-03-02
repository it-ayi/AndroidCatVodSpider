package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.OkHttp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class appYs extends Spider {

    private String siteUrl;

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "okhttp/3.12.11");
        return headers;
    }

    private JsonObject getResponse(String url) {
        try {
            String response = OkHttp.string(siteUrl + url, getHeaders());
            return Json.parse(response).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject(); // 返回空 JSON 对象以避免 NullPointerException
        }
    }

    @Override
    public void init(Context context, String ext) throws Exception {
        JsonObject config = Json.parse(ext).getAsJsonObject();
        siteUrl = config.get("site_url").getAsString();
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            String url = "videos.php/getlistvodeos"; // 获取视频列表的API
            JsonObject obj = getResponse(url);
            List<Class> classes = new ArrayList<>(); // 假设您有分类数据需要处理

            // 获取过滤器
            LinkedHashMap<String, List<Filter>> filters = getFilters();

            return Result.string(classes, filters);
        } catch (Exception e) {
            return Result.error("获取首页数据失败: " + e.getMessage());
        }
    }

    private LinkedHashMap<String, List<Filter>> getFilters() {
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        try {
            // 假设有一个获取过滤器的接口
            String url = "videos.php/getfilters";
            JsonObject obj = getResponse(url);
            JsonObject data = obj.getAsJsonObject("data");

            // 处理分类过滤器
            if (data.has("categories")) {
                List<Filter> categoryFilters = new ArrayList<>();
                JsonArray categories = data.getAsJsonArray("categories");
                for (int i = 0; i < categories.size(); i++) {
                    JsonObject category = categories.get(i).getAsJsonObject();
                    categoryFilters.add(new Filter(category.get("id").getAsString(), category.get("name").getAsString()));
                }
                filters.put("分类", categoryFilters);
            }

            // 处理年份过滤器
            if (data.has("years")) {
                List<Filter> yearFilters = new ArrayList<>();
                JsonArray years = data.getAsJsonArray("years");
                for (int i = 0; i < years.size(); i++) {
                    JsonObject year = years.get(i).getAsJsonObject();
                    yearFilters.add(new Filter(year.get("year").getAsString(), year.get("year").getAsString()));
                }
                filters.put("年份", yearFilters);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return filters;
    }

    @Override
    public String homeVideoContent() {
        try {
            String url = "videos.php/getlistvodeos"; // 获取视频列表
            JsonObject obj = getResponse(url);
            List<Vod> list = parseVideos(obj.getAsJsonArray("data"));
            return Result.string(list);
        } catch (Exception e) {
            return Result.error("获取视频数据失败: " + e.getMessage());
        }
    }

    private List<Vod> parseVideos(JsonArray videos) {
        List<Vod> list = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {
            JsonObject videoObj = videos.get(i).getAsJsonObject();
            Vod vod = new Vod();
            vod.setVodId(videoObj.get("视频ID").getAsInt());
            vod.setVodName(videoObj.get("视频标题").getAsString());
            vod.setVodPic(videoObj.get("cover_url").getAsString());
            vod.setVodDesc(videoObj.get("description").getAsString());
            vod.setVodYear(videoObj.get("year").getAsInt());
            vod.setVodDirector(videoObj.get("director").getAsString());
            vod.setVodActor(videoObj.get("actors").getAsString());
            vod.setType(videoObj.get("type").getAsString());
            list.add(vod);
        }
        return list;
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            String url = "videos.php/getvodeo/id/" + ids.get(0);
            JsonObject data = getResponse(url);
            List<Vod> vodList = parsePlaySources(data);
            return Result.string(vodList);
        } catch (Exception e) {
            return Result.error("获取视频详情失败: " + e.getMessage());
        }
    }

    private List<Vod> parsePlaySources(JsonObject data) {
        List<Vod> vodList = new ArrayList<>();
        JsonArray playSources = data.getAsJsonArray("play_sources");
        for (int i = 0; i < playSources.size(); i++) {
            JsonObject source = playSources.get(i).getAsJsonObject();
            Vod vod = new Vod();
            vod.setVodId(data.get("video_id").getAsInt());
            vod.setVodName(data.get("视频标题").getAsString());
            vod.setVodPlayFrom(source.get("source_name").getAsString());
            vod.setVodPlayUrl(source.get("play_url").getAsString());
            vodList.add(vod);
        }
        return vodList;
    }

    @Override
    public String searchContent(String wd) {
        try {
            String url = "videos.php/search?text=" + wd;
            JsonObject obj = getResponse(url);
            List<Vod> list = parseVideos(obj.getAsJsonArray("data"));
            return Result.string(list);
        } catch (Exception e) {
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}
