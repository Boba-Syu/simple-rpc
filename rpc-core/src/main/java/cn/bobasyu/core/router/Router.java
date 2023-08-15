package cn.bobasyu.core.router;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.registry.URL;

/**
 * 路由接口，用于路由选择
 */
public interface Router {
    /**
     * 刷新路由数组
     *
     * @param selector
     */
    void refreshRouterArr(Selector selector);

    /**
     * 获取请求的连接通道
     *
     * @param selector
     * @return
     */
    ChannelFutureWrapper select(Selector selector);

    /**
     * 更新权重信息
     *
     * @param url
     */
    void updateWeight(URL url);
}
