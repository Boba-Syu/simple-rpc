package cn.bobasyu.core.common.event;

import cn.bobasyu.core.common.event.RpcEvent;

public class RpcNodeChangeEvent implements RpcEvent {
    private Object object;

    public RpcNodeChangeEvent(Object object) {
        this.object = object;
    }

    @Override
    public Object getData() {
        return object;
    }

    @Override
    public RpcEvent setData(Object object) {
        this.object = object;
        return this;
    }
}
