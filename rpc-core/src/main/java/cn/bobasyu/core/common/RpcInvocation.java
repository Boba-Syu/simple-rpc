package cn.bobasyu.core.common;

/**
 * 远程调用的方法信息，封装了请求方法的参数和返回值等信息
 */
public class RpcInvocation {
    /**
     * 请求的目标方法
     */
    private String targetMethod;
    /**
     * 亲求的目标服务器
     */
    private String targetServiceName;
    /**
     * 请求参数信息
     */
    private Object[] args;
    /**
     * 用于匹配请求和响应的关键值，通过uuid来匹配对应的请求线程
     * 当请求从客户端发出的时候，会有一个uuid用于记录发出的请求，待数据返回的时候通过uuid来匹配对应的请求线程，并且返回给调用线程
     */
    private String uuid;
    /**
     * 接口响应的数据，如果为void则为null
     */
    private Object response;

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public String getTargetServiceName() {
        return targetServiceName;
    }

    public void setTargetServiceName(String targetServiceName) {
        this.targetServiceName = targetServiceName;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}
