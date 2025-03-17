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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BiliYs extends Spider {
    // API 主机地址
    private static String siteUrl = "https://api.bilibili.com";

    // 获取请求头
    private Map<String, String> getHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        header.put("Referer", "https://www.bilibili.com");
        // 假设 Cookie 在某个地方存储，您可以根据需要调整
        header.put("Cookie", "bili_jct=your_bili_jct; DedeUserID=your_dede_user_id; SESSDATA=your_sessdata;");
        return header;
    }

    // 初始化方法
    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        // 解析 extend 字符串或其它初始化逻辑
    }

    // 获取首页内容的方法
    @Override
    public String homeContent(boolean filter) throws Exception {
        // 构建请求的 URL，获取排行榜视频
        String url = siteUrl + "/pgc/web/rank/list?season_type=1&pagesize=20&page=1&day=3";
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        // 初始化类别、过滤器和视频列表
        List<Class> classes = new ArrayList<>();
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        List<Vod> list = new ArrayList<>();

        // 处理类别（示例，这部分需要根据实际 API 返回调整）
        classes.add(new Class("1", "番剧"));
        classes.add(new Class("4", "国创"));
        classes.add(new Class("2", "电影"));
        classes.add(new Class("5", "电视剧"));
        classes.add(new Class("3", "纪录片"));
        classes.add(new Class("7", "综艺"));

        // 处理视频列表
        JSONArray vodArray = jsonResponse.getJSONArray("result").getJSONObject(0).getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            // 创建视频对象并添加到列表中
            list.add(new Vod(
                    vodObj.getString("season_id"), // 视频 ID
                    vodObj.getString("title"),      // 视频标题
                    vodObj.getString("cover"),      // 视频封面
                    vodObj.has("new_ep") ? vodObj.getJSONObject("new_ep").getString("index_show") : vodObj.getString("index_show") // 视频备注
            ));
        }

        // 返回结果，包含类别、视频列表和过滤器
        return Result.string(classes, list, filters);
    }

    // 获取分类内容的方法
    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        // 构建请求 URL，获取指定分类的排行榜视频
        String url = String.format(siteUrl + "/pgc/web/rank/list?season_type=%s&pagesize=20&page=%s&day=3", tid, pg);
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        // 初始化视频列表
        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonResponse.getJSONArray("result").getJSONObject(0).getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            // 创建视频对象并添加到列表中
            list.add(new Vod(
                    vodObj.getString("season_id"),
                    vodObj.getString("title"),
                    vodObj.getString("cover"),
                    vodObj.has("new_ep") ? vodObj.getJSONObject("new_ep").getString("index_show") : vodObj.getString("index_show")
            ));
        }

        // 返回视频列表
        return Result.string(list);
    }

    // 获取视频详情的方法
    @Override
    public String detailContent(List<String> ids) throws Exception {
        // 构建请求 URL，获取指定视频的详细信息
        String url = String.format(siteUrl + "/pgc/view/web/season?season_id=%s", ids.get(0));
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        // 检查返回的状态码
        if (jsonResponse.getInt("code") == 0) {
            JSONObject vodObj = jsonResponse.getJSONObject("result");
            Vod vod = new Vod();
            // 从 JSON 中提取视频的详细信息
            vod.setVodId(vodObj.getString("season_id"));
            vod.setVodName(vodObj.getString("title"));
            vod.setVodPic(vodObj.getString("cover"));
            vod.setVodYear(vodObj.getJSONObject("publish").getString("pub_time").substring(0, 4)); // 提取年份
            vod.setVodArea(vodObj.getJSONArray("areas").getJSONObject(0).getString("name")); // 提取地区
            vod.setVodRemarks(vodObj.has("new_ep") ? vodObj.getJSONObject("new_ep").getString("index_show") : vodObj.getString("index_show")); // 视频备注
            vod.setVodContent(vodObj.getString("evaluate")); // 提取评价

            // 获取播放链接
            JSONArray episodes = vodObj.getJSONArray("episodes");
            StringBuilder playUrls = new StringBuilder();
            for (int i = 0; i < episodes.length(); i++) {
                JSONObject episode = episodes.getJSONObject(i);
                playUrls.append(episode.getString("title")).append("$").append(episode.getString("link")).append("#"); // 构建播放链接
            }

            vod.setVodPlayFrom("bilibili"); // 设置播放来源
            vod.setVodPlayUrl(playUrls.toString()); // 设置播放链接

            // 返回视频详细信息
            return Result.string(vod);
        }

        // 返回错误信息
        return Result.error("视频内容未找到");
    }

    // 搜索视频的方法
    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        // 构建请求 URL，进行视频搜索
        String url = String.format(siteUrl + "/x/web-interface/search/type?keyword=%s&page=1&search_type=1", key);
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        // 初始化视频列表
        List<Vod> list = new ArrayList<>();
        if (jsonResponse.getInt("code") == 0) {
            JSONArray vodArray = jsonResponse.getJSONArray("data"); // 提取搜索结果
            for (int i = 0; i < vodArray.length(); i++) {
                JSONObject vodObj = vodArray.getJSONObject(i);
                list.add(new Vod(
                        vodObj.getString("season_id"), // 视频 ID
                        vodObj.getString("title"),      // 视频标题
                        vodObj.getString("cover"),      // 视频封面
                        vodObj.getString("index_show")  // 视频备注
                ));
            }
        }

        // 返回搜索结果
        return Result.string(list);
    }

    // 获取播放地址的方法
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        // 构建请求 URL，获取视频的播放地址
        String url = siteUrl + String.format("/pgc/view/web/season?season_id=%s", id);
        JSONObject jsonResponse = new JSONObject(OkHttp.string(url, getHeader()));

        // 检查返回的状态码
        if (jsonResponse.getInt("code") == 0) {
            // 获取播放链接
            String videoUrl = jsonResponse.getJSONObject("result").getJSONArray("episodes").getJSONObject(0).getString("link");
            return Result.get().url(videoUrl).header(getHeader()).string(); // 返回播放地址
        }

        // 返回错误信息
        return Result.error("播放地址未找到");
    }
}
