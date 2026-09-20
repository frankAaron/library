package com.library.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageUtil {

    public static <T> Map<String, Object> toMap(List<T> list, long total, int pageNum, int pageSize) {
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("total", total);
        map.put("pageNum", pageNum);
        map.put("pageSize", pageSize);
        map.put("pages", pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
        return map;
    }

    public static int offset(int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        return (pageNum - 1) * pageSize;
    }
}
