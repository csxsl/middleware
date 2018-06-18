package com.alibaba.dubbo.performance.demo.agent.agent.model;/**
 * Created by msi- on 2018/6/4.
 */

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-06-04 10:14
 **/

public class Invocation {
    private String interfaceName;
    private String method;
    private String parameterTypesString;
    private String parameter;

    public Invocation(String interfaceName, String method, String parameterTypesString, String parameter) {
        this.interfaceName = interfaceName;
        this.method = method;
        this.parameterTypesString = parameterTypesString;
        this.parameter = parameter;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMethod() {
        return method;
    }

    public String getParameterTypesString() {
        return parameterTypesString;
    }

    public String getParameter() {
        return parameter;
    }
}
