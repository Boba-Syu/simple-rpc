package cn.bobasyu.core.registry.zookeeper;

import cn.bobasyu.core.common.event.RpcNodeChangeEvent;
import cn.bobasyu.core.common.event.RpcEvent;
import cn.bobasyu.core.common.event.RpcListenerLoader;
import cn.bobasyu.core.common.event.RpcUpdateEvent;
import cn.bobasyu.core.common.event.data.URLChangeWrapper;
import cn.bobasyu.core.registry.RegistryService;
import cn.bobasyu.core.registry.URL;
import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 * Zookeeper注册，包括完成服务注册，服务订阅，服务下线等操作
 */
public class ZookeeperRegister extends AbstractRegister implements RegistryService {

    private AbstractZookeeperClient zkClient;

    private String ROOT = "/irpc";

    public ZookeeperRegister(String address) {
        this.zkClient = new CuratorZookeeperClient(address);
    }

    private String getProviderPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/provider/" + url.getParameters().get("host") + ":" + url.getParameters().get("port");
    }

    private String getConsumerPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/consumer/" + url.getApplicationName() + ":" + url.getParameters().get("host") + ":";
    }

    @Override
    public List<String> getProviderIps(String serviceName) {
        return this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
    }


    @Override
    public void register(URL url) {
        if (!this.zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildProviderUrlStr(url);
        if (zkClient.existNode(getProviderPath(url))) {
            zkClient.deleteNode(getProviderPath(url));
        }
        zkClient.createTemporaryData(getProviderPath(url), urlStr);
        super.register(url);
    }

    @Override
    public void unRegister(URL url) {
        zkClient.deleteNode(getProviderPath(url));
        super.unRegister(url);
    }

    @Override
    public void subscribe(URL url) {
        if (!this.zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildConsumerUrlStr(url);
        if (zkClient.existNode(getConsumerPath(url))) {
            zkClient.deleteNode(getConsumerPath(url));
        }
        zkClient.createTemporarySeqData(getConsumerPath(url), urlStr);
        super.subscribe(url);
    }

    @Override
    public void doAfterSubscribe(URL url) {
        //监听是否有新的服务注册
        String servicePath = url.getParameters().get("servicePath");
        String newServerNodePath = ROOT + "/" + url.getServiceName() + "/provider";
        watchChildNodeData(newServerNodePath);
        String providerIpStrJson = url.getParameters().get("providerIps");
        List<String> providerIpList = JSON.parseObject(providerIpStrJson, List.class);
        for (String providerIp : providerIpList) {
            this.watchNodeDataChange(ROOT + "/" + servicePath + "/" + providerIp);
        }
    }

    public void watchNodeDataChange(String newServerNodePath) {
        zkClient.watchChildNodeData(newServerNodePath, watchedEvent -> {
            String path = watchedEvent.getPath();
            String nodeData = zkClient.getNodeData(path);
            ProviderNodeInfo providerNodeInfo = URL.buildURLFromUrlStr(nodeData);
            RpcEvent iRpcEvent = new RpcNodeChangeEvent(providerNodeInfo);
            RpcListenerLoader.sendEvent(iRpcEvent);
            watchNodeDataChange(newServerNodePath);
        });
    }

    public void watchChildNodeData(String newServerNodePath) {
        zkClient.watchChildNodeData(newServerNodePath, watchedEvent -> {
            System.out.println(watchedEvent);
            String path = watchedEvent.getPath();
            List<String> childrenDataList = zkClient.getChildrenData(path);
            URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
            urlChangeWrapper.setProviderUrl(childrenDataList);
            urlChangeWrapper.setServiceName(path.split("/")[2]);
            // 自定义的一套事件监听组件
            RpcEvent iRpcEvent = new RpcUpdateEvent(urlChangeWrapper);
            RpcListenerLoader.sendEvent(iRpcEvent);
            // 收到回调之后在注册一次监听，这样能保证一直都收到消息
            watchChildNodeData(path);
        });
    }

    @Override
    public void doBeforeSubscribe(URL url) {

    }

    @Override
    public void doUnSubscribe(URL url) {
        this.zkClient.deleteNode(getConsumerPath(url));
        super.doUnSubscribe(url);
    }
}
