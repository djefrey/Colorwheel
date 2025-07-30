package dev.djefrey.colorwheel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AccumulateTimer
{
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final float delay;

    private ScheduledFuture<?> future = null;

    public AccumulateTimer(float delay)
    {
        this.delay = delay;
    }

    public void request(Runnable task)
    {
        if (future != null && !future.isDone())
        {
            future.cancel(false);
        }

        future = scheduler.schedule(task, (long) (delay * 1000000), TimeUnit.MICROSECONDS);
    }
}
