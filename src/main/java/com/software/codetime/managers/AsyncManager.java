package com.software.codetime.managers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class AsyncManager {
    private static AsyncManager instance = null;
    public static final Logger log = Logger.getLogger("AsyncManager");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<String> names = new ArrayList();
    private final List<Future<?>> futures = new ArrayList();

    public AsyncManager() {
    }

    public static AsyncManager getInstance() {
        if (instance == null) {
            synchronized(log) {
                if (instance == null) {
                    instance = new AsyncManager();
                }
            }
        }

        return instance;
    }

    public void scheduleService(Runnable service, String name, int delayBeforeExecute, int interval) {
        if (!this.names.contains(name)) {
            Future<?> future = this.scheduler.scheduleAtFixedRate(service, delayBeforeExecute, interval, TimeUnit.SECONDS);
            this.futures.add(future);
        }

    }

    public ScheduledFuture executeOnceInSeconds(Runnable service, long delayInSeconds) {
        return this.scheduler.schedule(service, delayInSeconds, TimeUnit.SECONDS);
    }

    public void destroyServices() {
        if (this.futures.size() > 0) {
            Iterator var1 = this.futures.iterator();

            while(var1.hasNext()) {
                Future<?> future = (Future)var1.next();

                try {
                    future.cancel(true);
                } catch (Exception var4) {
                }
            }
        }

    }
}
