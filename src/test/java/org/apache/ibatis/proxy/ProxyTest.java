package org.apache.ibatis.proxy;

import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ProxyTest {


    @Test
    public void testProxy(){

        People people=new Teacher();

        InvocationHandler handler=new WorkHandler(people);

        People proxy= (People) Proxy.newProxyInstance(handler.getClass().getClassLoader(),people.getClass().getInterfaces(),handler);

        System.out.println(proxy.work());

    }

}
