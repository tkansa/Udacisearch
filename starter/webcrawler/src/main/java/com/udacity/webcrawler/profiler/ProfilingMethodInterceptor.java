package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private Object target;
    private final Clock clock;
    private ProfilingState profilingState;

    // TODO: You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(Object target, Clock clock, ProfilingState profilingState) {
        this.clock = Objects.requireNonNull(clock);
        this.target = Objects.requireNonNull(target);
        this.profilingState = Objects.requireNonNull(profilingState);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable, IllegalAccessException {
        // TODO: This method interceptor should inspect the called method to see if it is a profiled
        //       method. For profiled methods, the interceptor should record the start time, then
        //       invoke the method using the object that is being profiled. Finally, for profiled
        //       methods, the interceptor should record how long the method call took, using the
        //       ProfilingState methods.


        Object result = proxy;
        Instant start = null;
        boolean isProfiled = method.isAnnotationPresent(Profiled.class);
        if (isProfiled) {
            start = clock.instant();

        }

        try {
            result = method.invoke(target, args);
        }  catch (IllegalAccessException e) {
                throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        finally {
            if(isProfiled){
                Duration duration = Duration.between(start, clock.instant());
                profilingState.record(target.getClass(), method, duration);
            }
        }


        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfilingMethodInterceptor)) return false;
        ProfilingMethodInterceptor that = (ProfilingMethodInterceptor) o;
        return Objects.equals(this.clock, that.clock) && Objects.equals(this.target, that.target) && Objects.equals(this.profilingState, that.profilingState);

    }
}



