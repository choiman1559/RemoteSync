package com.sync.protocol.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author minhui.zhu
 *         Created by minhui.zhu on 2018/4/30.
 *         Copyright © 2017年 Oceanwing. All rights reserved.
 */

public class ThreadProxy {

    private final Executor executor;

    static class InnerClass {
        static ThreadProxy instance = new ThreadProxy();
    }

    private ThreadProxy() {

        executor = new ThreadPoolExecutor(1, 4,
                10L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024), r -> {
                    Thread thread = new Thread(r);
                    thread.setName("ThreadProxy");
                    return thread;
                });
    }
    public void execute(Runnable run){
        executor.execute(run);
    }
    public static ThreadProxy getInstance(){
        return InnerClass.instance;
    }
}
