package cn.bobasyu.core.common;

import java.util.concurrent.Semaphore;

/**
 * 封装有server单个服务的连接上限信息
 */
public class ServerServiceSemaphoreWrapper {
    /**
     * 信号量，记录当前；连接数
     **/
    private Semaphore semaphore;
    /**
     * 最大连接数
     **/
    private int maxNums;

    public ServerServiceSemaphoreWrapper(int maxNums) {
        this.semaphore = new Semaphore(maxNums);
        this.maxNums = maxNums;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public int getMaxNums() {
        return maxNums;
    }

    public void setMaxNums(int maxNums) {
        this.maxNums = maxNums;
    }
}
