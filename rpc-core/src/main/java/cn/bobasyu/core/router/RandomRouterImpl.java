package cn.bobasyu.core.router;

import cn.bobasyu.core.common.ChannelFutureWrapper;
import cn.bobasyu.core.registry.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static cn.bobasyu.core.common.cache.CommonClientCache.*;

/**
 * 随机调用路由
 */
public class RandomRouterImpl implements Router {

    @Override
    public void refreshRouterArr(Selector selector) {
        //获取服务提供者的数目
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
        ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        // 提前生成调用先后顺序的随机数组
        int[] result = createRandomIndex(arr.length);
        // 生成对应服务集群的每套机器的调用顺序
        for (int i = 0; i < result.length; ++i) {
            arr[i] = channelFutureWrappers.get(result[i]);
        }
        SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(), arr);
    }


    @Override
    public ChannelFutureWrapper select(Selector selector) {
        return CHANNEL_FUTURE_POLLING_REF.getChannelFutureWrapper(selector.getProviderServiceName());
    }

    @Override
    public void updateWeight(URL url) {
        // 服务节点的权重
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(url.getServiceName());
        Integer[] weightArr = createWeightArr(channelFutureWrappers);
        Integer[] finalArr = createRandomArr(weightArr);
        ChannelFutureWrapper[] finalChannelFutureWrappers = new ChannelFutureWrapper[finalArr.length];
        for (int i = 0; i < finalArr.length; ++i) {
            finalChannelFutureWrappers[i] = channelFutureWrappers.get(i);
        }
        SERVICE_ROUTER_MAP.put(url.getServiceName(), finalChannelFutureWrappers);
    }

    private Integer[] createRandomArr(Integer[] weightArr) {
        int total = weightArr.length;
        Random random = new Random();
        for (int i = 0; i < total; ++i) {
            int j = random.nextInt(total);
            if (i != j) {
                int temp = weightArr[i];
                weightArr[i] = weightArr[j];
                weightArr[j] = temp;
            }
        }
        return weightArr;
    }


    private Integer[] createWeightArr(List<ChannelFutureWrapper> channelFutureWrappers) {
        List<Integer> weightArr = new ArrayList<>();
        for (int i = 0; i < channelFutureWrappers.size(); ++i) {
            Integer weight = channelFutureWrappers.get(i).getWeight();
            int c = weight / 100;
            for (int j = 0; j < c; ++j) {
                weightArr.add(j);
            }
        }
        Integer[] arr = new Integer[weightArr.size()];
        return weightArr.toArray(arr);
    }

    private int[] createRandomIndex(int length) {
        int[] arrInt = new int[length];
        Random ra = new Random();
        for (int i = 0; i < arrInt.length; i++) {
            arrInt[i] = -1;
        }
        int index = 0;
        while (index < arrInt.length) {
            int num = ra.nextInt(length);
            //如果数组中不包含这个元素则赋值给数组
            if (!contains(arrInt, num)) {
                arrInt[index++] = num;
            }
        }
        return arrInt;
    }

    public boolean contains(int[] arr, int key) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == key) {
                return true;
            }
        }
        return false;
    }
}
