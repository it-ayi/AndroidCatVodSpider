package com.github.catvod.spider;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * CeChi 类实现了 Spider 接口，通过 API 获取视频数据并返回相应格式的数据。
 */
public class AppYsV2 extends Spider {
    private static String apiUrl = "http://yun.itayi.xyz"; // 替换为您的 API 基础 URL

    private HashMap<String, String> getHeader() {
        HashMap<String, String> header = new HashMap<>();
        header.put("User-Agent", Util.CHROME);
        return header;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        if (!TextUtils.isEmpty(extend)) {
            apiUrl = extend; // 如果有扩展参数，则使用它
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        String jsonResponse = OkHttp.string(apiUrl + "/homeContent?filter=" + filter, getHeader());
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // 解析分类信息
        List<Class> classes = new ArrayList<>();
        JSONArray classArray = jsonObject.getJSONArray("class");
        for (int i = 0; i < classArray.length(); i++) {
            JSONObject classObj = classArray.getJSONObject(i);
            classes.add(new Class(classObj.getString("type_id"), classObj.getString("type_name")));
        }

        // 解析视频列表
        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonObject.getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            ));
        }

        // 解析过滤器信息
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        JSONObject filtersObject = jsonObject.getJSONObject("filters");
        for (String key : filtersObject.keys()) {
            JSONArray filterArray = filtersObject.getJSONArray(key);
            List<Filter> filterList = new ArrayList<>();
            for (int j = 0; j < filterArray.length(); j++) {
                JSONObject filterObj = filterArray.getJSONObject(j);
                String filterId = filterObj.getString("key");
                String filterName = filterObj.getString("name");
                List<Filter.Value> filterValues = new ArrayList<>();
                JSONArray valueArray = filterObj.getJSONArray("value");
                for (int k = 0; k < valueArray.length(); k++) {
                    JSONObject valueObj = valueArray.getJSONObject(k);
                    filterValues.add(new Filter.Value(valueObj.getString("n"), valueObj.getString("v")));
                }
                filterList.add(new Filter(filterId, filterName, filterValues));
            }
            filters.put(key, filterList);
        }

        return Result.string(classes, list, filters);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String jsonResponse = OkHttp.string(apiUrl + "/categoryContent?tid=" + tid + "&pg=" + pg, getHeader());
        JSONObject jsonObject = new JSONObject(jsonResponse);

        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonObject.getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            ));
        }

        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String jsonResponse = OkHttp.string(apiUrl + "/detailContent?ids=" + JSON.toJSONString(ids), getHeader());
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // 解析视频详细信息
        List<Vod> vodList = new ArrayList<>();
        JSONArray listArray = jsonObject.getJSONArray("list");
        for (int i = 0; i < listArray.length(); i++) {
            JSONObject vodObj = listArray.getJSONObject(i);
            Vod vod = new Vod();
            vod.setVodId(vodObj.getString("vod_id"));
            vod.setVodName(vodObj.getString("vod_name"));
            vod.setVodPic(vodObj.getString("vod_pic"));
            vod.setVodYear(vodObj.getString("vod_year"));
            vod.setVodRemarks(vodObj.getString("vod_remarks"));
            vod.setVodActor(vodObj.getString("vod_actor"));
            vod.setVodDirector(vodObj.getString("vod_director"));
            vod.setVodContent(vodObj.getString("vod_content"));
            vod.setVodPlayFrom(vodObj.getString("vod_play_from"));
            vod.setVodPlayUrl(vodObj.getString("vod_play_url"));
            vodList.add(vod);
        }

        return Result.string(vodList);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String jsonResponse = OkHttp.string(apiUrl + "/searchContent?key=" + URLEncoder.encode(key, "UTF-8") + "&quick=" + quick, getHeader());
        JSONObject jsonObject = new JSONObject(jsonResponse);

        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonObject.getJSONArray("list");
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i);
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            ));
        }

        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String jsonResponse = OkHttp.string(apiUrl + "/playerContent?id=" + id + "&flag=" + flag + (vipFlags.isEmpty() ? "" : "&vipFlags=" + JSON.toJSONString(vipFlags)), getHeader());
        JSONObject jsonObject = new JSONObject(jsonResponse);
        String realUrl = jsonObject.getString("url");

        return Result.get().url(realUrl).header(getHeader()).string();
    }
}
