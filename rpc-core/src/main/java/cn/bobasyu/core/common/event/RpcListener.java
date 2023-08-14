package cn.bobasyu.core.common.event;

/**
 * 事件监听器
 *
 * @param <T>
 */
public interface RpcListener<T> {
    void callback(Object t);
}
