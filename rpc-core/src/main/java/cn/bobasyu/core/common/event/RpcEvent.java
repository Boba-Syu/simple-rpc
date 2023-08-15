package cn.bobasyu.core.common.event;

/**
 * 事件接口，用于装在需要传递的数据信息
 * 当zookeeper的某个节点发生数据变动时，就会发送一个变更时间，然后由对应的监听器去捕获这些数据并做处理
 */
public interface RpcEvent {

    Object getData();

    RpcEvent setData(Object data);
}
