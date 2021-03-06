package com.kalix.framework.osgi.api;

import com.kalix.framework.core.api.IService;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Map;

/**
 * @author chenyanxu
 */
public interface IBundleService extends IService {
    /**
     * 启动一个app
     *
     * @param id appId
     * @return 返回信息
     */
    Map startApp(String id);

    /**
     * 停止一个app
     * @param id appId
     * @return 返回信息
     */
    Map stopApp(String id);

    /**
     * 获得一个app的当前状态
     * @param appIds appIds以 _ 分隔
     * @return 返回信息
     */
    Map getAppStatus(String appIds);//connect by '_'

    /**
     * 根据过滤条件查询Bundle列表
     * @param filter
     * @return
     */
    List<Bundle> getBundleList(String filter);
}
