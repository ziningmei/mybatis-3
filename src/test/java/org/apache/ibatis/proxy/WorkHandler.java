package org.apache.ibatis.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class WorkHandler implements InvocationHandler {

    Object object;

    public WorkHandler(Object object) {
        this.object = object;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("before invoke");

        Object invoke=method.invoke(object,args);

        System.out.println("after invoke");
        return invoke;
    }
}
