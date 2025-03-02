package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

/**
 * AppYsV2 类是一个基于 CatVod 的视频爬虫类
 * 用于获取视频数据、详情、搜索和分类内容。
 */
public class AppYsV2 extends Spider {

    private String serverUrl; // 存储服务器 URL
    private String mackey; // 存储 mackey
    private String ali; // 存储 ali
    private String quark; // 存储 quark
    private String uc; // 存储 uc

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        JSONObject extJson = new JSONObject(extend); // 将传入的字符串转换为 JSON 对象
        JSONObject ext = extJson.getJSONObject("ext"); // 获取 "ext" 对象

        // 解析各个参数
        serverUrl = ext.getString("server"); // 获取 server URL
        mackey = ext.optString("mackey", ""); // 获取 mackey
        ali = ext.optString("ali", ""); // 获取 ali
        quark = ext.optString("quark", ""); // 获取 quark
        uc = ext.optString("uc", ""); // 获取 uc
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        String url = serverUrl + "getlistvideos"; // 使用解析后的 URL
        SpiderDebug.log(url); // 打印调试信息
        String json = OkHttp.string(url, getHeaders(url)); // 发起请求，获取 JSON 数据
        JSONObject obj = new JSONObject(json); // 转换为 JSONObject
        JSONArray videos = new JSONArray(); // 用于存储视频列表

        // 检查返回的状态码
        if (obj.getInt("code") == 0) {
            JSONArray dataArray = obj.getJSONArray("data"); // 获取视频数据数组
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject videoObj = dataArray.getJSONObject(i); // 获取每个视频对象
                JSONObject video = new JSONObject(); // 创建用于存储视频信息的 JSON 对象
                video.put("vod_id", videoObj.getString("video_id")); // 视频 ID
                video.put("vod_name", videoObj.getString("视频标题")); // 视频标题
                video.put("vod_pic", videoObj.optString("cover_url", "")); // 视频封面 URL
                video.put("vod_remarks", videoObj.optString("description", "")); // 视频描述
                videos.put(video); // 将视频信息添加到视频数组中
            }
        }

        JSONObject result = new JSONObject(); // 创建结果 JSON 对象
        result.put("list", videos); // 将视频列表添加到结果中
        return result.toString(); // 返回结果的字符串形式
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String videoId = ids.get(0); // 获取请求的第一个视频 ID
        String url = serverUrl + "getvideo/" + videoId; // 获取视频详情的 API URL
        SpiderDebug.log(url); // 打印调试信息
        String json = OkHttp.string(url, getHeaders(url)); // 发起请求，获取视频详情 JSON 数据
        JSONObject obj = new JSONObject(json); // 转换为 JSONObject
        JSONObject result = new JSONObject(); // 创建结果 JSON 对象

        // 检查返回的状态码
        if (obj.getInt("code") == 0) {
            JSONObject data = obj.getJSONObject("data"); // 获取视频数据对象
            JSONObject vod = new JSONObject(); // 创建新的 JSON 对象存储视频详情
            vod.put("vod_id", data.getString("id")); // 视频 ID
            vod.put("vod_name", data.getString("title")); // 视频标题
            vod.put("vod_pic", data.optString("cover_url", "")); // 视频封面 URL
            vod.put("vod_year", data.optString("year", "")); // 视频年份
            vod.put("vod_area", data.optString("category_id", "")); // 视频分类 ID
            vod.put("vod_remarks", data.optString("description", "")); // 视频描述
            vod.put("vod_actor", data.optString("actors", "")); // 演员信息
            vod.put("vod_director", data.optString("director", "")); // 导演信息

            // 获取播放源
            JSONArray playSources = data.getJSONArray("play_sources");
            StringBuilder playFrom = new StringBuilder(); // 播放源名称的 StringBuilder
            StringBuilder playUrl = new StringBuilder(); // 播放源 URL 的 StringBuilder

            // 遍历播放源
            for (int i = 0; i < playSources.length(); i++) {
                JSONObject source = playSources.getJSONObject(i); // 获取播放源对象
                if (i > 0) {
                    playFrom.append("$$$"); // 添加分隔符
                    playUrl.append("$$$"); // 添加分隔符
                }
                playFrom.append(source.getString("video_title")); // 添加播放源名称
                playUrl.append(source.getString("play_url")); // 添加播放源 URL
            }

            // 将播放源信息添加到视频详情中
            vod.put("vod_play_from", playFrom.toString());
            vod.put("vod_play_url", playUrl.toString());
            JSONArray list = new JSONArray(); // 创建列表数组
            list.put(vod); // 将视频信息添加到列表中
            result.put("list", list); // 将列表添加到结果中
        }

        return result.toString(); // 返回结果的字符串形式
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        // 编码搜索关键字并构造搜索 API 的 URL
        String url = serverUrl + "search?text=" + URLEncoder.encode(key, "UTF-8");
        SpiderDebug.log(url); // 打印调试信息
        String json = OkHttp.string(url, getHeaders(url)); // 发起请求，获取搜索结果 JSON 数据
        JSONObject obj = new JSONObject(json); // 转换为 JSONObject
        JSONArray videos = new JSONArray(); // 用于存储搜索结果

        // 检查返回的状态码
        if (obj.getInt("code") == 0) {
            JSONArray dataArray = obj.getJSONArray("data"); // 获取搜索结果数组
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject videoObj = dataArray.getJSONObject(i); // 获取每个视频对象
                JSONObject video = new JSONObject(); // 创建新的 JSON 对象存储视频信息
                video.put("vod_id", videoObj.getString("video_id")); // 视频 ID
                video.put("vod_name", videoObj.getString("视频标题")); // 视频标题
                video.put("vod_pic", videoObj.optString("cover_url", "")); // 视频封面 URL
                video.put("vod_remarks", videoObj.optString("description", "")); // 视频描述
                videos.put(video); // 将视频信息添加到搜索结果数组中
            }
        }

        JSONObject result = new JSONObject(); // 创建结果 JSON 对象
        result.put("list", videos); // 将搜索结果添加到结果中
        return result.toString(); // 返回结果的字符串形式
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        // 获取分类视频的 API URL
        String url = serverUrl + "getvideosbycategory/" + tid; 
        SpiderDebug.log(url); // 打印调试信息
        String json = OkHttp.string(url, getHeaders(url)); // 发起请求，获取分类视频 JSON 数据
        JSONObject obj = new JSONObject(json); // 转换为 JSONObject
        JSONArray videos = new JSONArray(); // 用于存储分类视频列表

        // 检查返回的状态码
        if (obj.getInt("code") == 0) {
            JSONArray dataArray = obj.getJSONArray("data"); // 获取分类视频数组
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject videoObj = dataArray.getJSONObject(i); // 获取每个视频对象
                JSONObject video = new JSONObject(); // 创建新的 JSON 对象存储视频信息
                video.put("vod_id", videoObj.getString("video_id")); // 视频 ID
                video.put("vod_name", videoObj.getString("视频标题")); // 视频标题
                video.put("vod_pic", videoObj.optString("cover_url", "")); // 视频封面 URL
                video.put("vod_remarks", videoObj.optString("description", "")); // 视频描述
                videos.put(video); // 将视频信息添加到分类视频数组中
            }
        }

        JSONObject result = new JSONObject(); // 创建结果 JSON 对象
        result.put("list", videos); // 将分类视频列表添加到结果中
        return result.toString(); // 返回结果的字符串形式
    }

    // 获取请求头信息
    private HashMap<String, String> getHeaders(String URL) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", UA(URL)); // 设置 User-Agent
        return headers; // 返回请求头
    }

    // 设置 User-Agent
    private String UA(String URL) {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    }
}
