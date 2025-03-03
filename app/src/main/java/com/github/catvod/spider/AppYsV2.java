package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CeChi 类实现了 Spider 接口，通过 API 获取视频数据并返回相应格式的数据。
 */
public class AppYsV2 extends Spider {
    private static final String BASE_URL = "https://yun.itayi.xyz"; // 替换为您的 API 基础 URL
    private final OkHttpClient client = new OkHttpClient(); // 创建 OkHttp 客户端实例

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend); // 调用父类的初始化方法
    }

    /**
     * 发送 GET 请求获取指定 API 端点的响应数据
     *
     * @param endpoint API 的特定路径
     * @return 返回 API 响应的 JSON 字符串
     * @throws IOException 如果请求失败或连接出现问题
     */
    private String getApiResponse(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .headers(getHeader()) // 设置请求头
                .build(); // 构建请求
        try (Response response = client.newCall(request).execute()) { // 发送请求并获取响应
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response); // 检查响应是否成功
            return response.body().string(); // 返回响应体的内容
        }
    }

    /**
     * 获取请求头信息
     *
     * @return 返回包含请求头的 Headers 对象
     */
    private Headers getHeader() {
        return new Headers.Builder()
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .add("Host", "yun.itayi.xyz")
                .build();
    }

    /**
     * 将 Headers 转换为 Map
     *
     * @param headers Headers 对象
     * @return 转换后的 Map
     */
    private Map<String, String> headersToMap(Headers headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.name(i), headers.value(i));
        }
        return map;
    }

    /**
     * 获取首页内容，包括视频列表和分类信息
     *
     * @param filter 是否进行过滤
     * @return 返回首页内容的 JSON 字符串
     * @throws Exception 处理请求或解析时的异常
     */
    @Override
    public String homeContent(boolean filter) throws Exception {
        String jsonResponse = getApiResponse("/homeContent?filter=" + filter); // 获取首页数据的 API 响应
        JSONObject jsonObject = new JSONObject(jsonResponse); // 将响应解析为 JSON 对象

        // 解析分类信息
        List<Class> classes = new ArrayList<>();
        JSONArray classArray = jsonObject.getJSONArray("class"); // 获取分类数组
        for (int i = 0; i < classArray.length(); i++) {
            JSONObject classObj = classArray.getJSONObject(i); // 获取每个分类对象
            classes.add(new Class(classObj.getString("type_id"), classObj.getString("type_name"))); // 添加到分类列表
        }

        // 解析视频列表
        List<Vod> list = new ArrayList<>();
        JSONArray vodArray = jsonObject.getJSONArray("list"); // 获取视频数组
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i); // 获取每个视频对象
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            )); // 添加到视频列表
        }

        // 解析过滤器信息
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        JSONObject filtersObject = jsonObject.getJSONObject("filters"); // 获取过滤器对象
        for (Iterator<String> it = filtersObject.keys(); it.hasNext(); ) { // 使用 Iterator 遍历
            String key = it.next(); // 获取下一个键
            JSONArray filterArray = filtersObject.getJSONArray(key);
            List<Filter> filterList = new ArrayList<>();
            for (int j = 0; j < filterArray.length(); j++) {
                JSONObject filterObj = filterArray.getJSONObject(j);
                String filterId = filterObj.getString("key");
                String filterName = filterObj.getString("name");
                List<Filter.Value> filterValues = new ArrayList<>();
                JSONArray valueArray = filterObj.getJSONArray("value"); // 获取过滤器值数组
                for (int k = 0; k < valueArray.length(); k++) {
                    JSONObject valueObj = valueArray.getJSONObject(k); // 获取每个过滤器值对象
                    filterValues.add(new Filter.Value(valueObj.getString("name"), valueObj.getString("value"))); // 添加到过滤器值列表
                }
                filterList.add(new Filter(filterId, filterName, filterValues)); // 将过滤器添加到过滤器列表
            }
            filters.put(key, filterList); // 将过滤器添加到过滤器映射
        }

        return Result.string(classes, list, filters); // 返回结果字符串
    }

    /**
     * 获取指定类别的分页视频内容
     *
     * @param tid    类别 ID
     * @param pg     页码
     * @param filter 是否进行过滤
     * @param extend 额外参数
     * @return 返回指定类别内容的 JSON 字符串
     * @throws Exception 处理请求或解析时的异常
     */
    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String jsonResponse = getApiResponse("/categoryContent?tid=" + tid + "&pg=" + pg + "&filter=" + filter); // 获取类别数据的 API 响应
        JSONObject jsonObject = new JSONObject(jsonResponse); // 解析响应为 JSON 对象

        List<Vod> list = new ArrayList<>(); // 创建视频列表
        JSONArray vodArray = jsonObject.getJSONArray("list"); // 获取视频数组
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i); // 获取每个视频对象
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            )); // 添加到视频列表
        }

        return Result.string(list); // 返回视频列表的结果字符串
    }

    /**
     * 获取视频的详细信息
     *
     * @param ids 视频 ID 列表
     * @return 返回视频详细信息的 JSON 字符串
     * @throws Exception 处理请求或解析时的异常
     */
    @Override
    public String detailContent(List<String> ids) throws Exception {
        JSONArray jsonArray = new JSONArray();
        for (String id : ids) {
            jsonArray.put(id);
        }
        String jsonResponse = getApiResponse("/detailContent?ids=" + jsonArray.toString()); // 获取视频详情的 API 响应
        JSONObject jsonObject = new JSONObject(jsonResponse); // 解析响应为 JSON 对象

        Vod vod = new Vod(); // 创建视频对象
        vod.setVodId(jsonObject.getString("vod_id")); // 设置视频 ID
        vod.setVodYear(jsonObject.getString("vod_year")); // 设置视频年份
        vod.setVodName(jsonObject.getString("vod_name")); // 设置视频名称
        vod.setVodActor(jsonObject.getString("vod_actor")); // 设置视频演员
        vod.setVodRemarks(jsonObject.getString("vod_remarks")); // 设置视频备注
        vod.setVodContent(jsonObject.getString("vod_content")); // 设置视频内容
        vod.setVodDirector(jsonObject.getString("vod_director")); // 设置视频导演
        vod.setVodPlayFrom(jsonObject.getString("vod_play_from")); // 设置播放来源
        vod.setVodPlayUrl(jsonObject.getString("vod_play_url")); // 设置播放链接

        return Result.string(vod); // 返回视频详细信息
    }

    /**
     * 根据关键词搜索视频
     *
     * @param key   搜索关键词
     * @param quick 是否快速搜索
     * @return 返回搜索结果的 JSON 字符串
     * @throws Exception 处理请求或解析时的异常
     */
    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String jsonResponse = getApiResponse("/searchContent?key=" + key + "&quick=" + quick); // 获取搜索数据的 API 响应
        JSONObject jsonObject = new JSONObject(jsonResponse); // 解析响应为 JSON 对象

        List<Vod> list = new ArrayList<>(); // 创建视频列表
        JSONArray vodArray = jsonObject.getJSONArray("list"); // 获取视频数组
        for (int i = 0; i < vodArray.length(); i++) {
            JSONObject vodObj = vodArray.getJSONObject(i); // 获取每个视频对象
            list.add(new Vod(
                    vodObj.getString("vod_id"),
                    vodObj.getString("vod_name"),
                    vodObj.getString("vod_pic"),
                    vodObj.getString("vod_remarks")
            )); // 添加到视频列表
        }

        return Result.string(list); // 返回搜索结果的结果字符串
    }

    /**
     * 获取视频播放链接
     *
     * @param flag     播放标识
     * @param id       视频 ID
     * @param vipFlags VIP 标识列表
     * @return 返回播放链接的 JSON 字符串
     * @throws Exception 处理请求或解析时的异常
     */
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        StringBuilder vipFlagsString = new StringBuilder();
        if (vipFlags != null && !vipFlags.isEmpty()) {
            for (String vipFlag : vipFlags) {
                if (vipFlagsString.length() != 0) {
                    vipFlagsString.append(",");
                }
                vipFlagsString.append(vipFlag);
            }
        }
        String jsonResponse = getApiResponse("/playerContent?id=" + id + "&flag=" + flag + (vipFlags.isEmpty() ? "" : "&vipFlags=[" + vipFlagsString + "]")); // 获取播放数据的 API 响应
        JSONObject jsonObject = new JSONObject(jsonResponse); // 解析响应为 JSON 对象
        String realUrl = jsonObject.getString("url"); // 获取真实播放链接

        return Result.get().url(realUrl).header(headersToMap(getHeader())).string(); // 返回播放链接及头信息
    }
}
