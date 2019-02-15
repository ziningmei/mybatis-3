package org.apache.ibatis.proxy;

public class Teacher implements People {


    @Override
    public String work() {
        System.out.println("work");

        return "teach";
    }
}
