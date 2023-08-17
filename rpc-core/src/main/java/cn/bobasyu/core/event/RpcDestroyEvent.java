package cn.bobasyu.core.event;

import cn.bobasyu.core.common.event.RpcEvent;

/**
 * 服务销毁事件
 */
public class RpcDestroyEvent implements RpcEvent {

    private Object data;

    public RpcDestroyEvent(Object data) {
        this.data = data;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public RpcEvent setData(Object data) {
        this.data = data;
        return this;
    }
}
