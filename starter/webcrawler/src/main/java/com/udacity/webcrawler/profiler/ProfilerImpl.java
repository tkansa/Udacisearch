package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    if(!profiledAnnotated(klass)){
      throw new IllegalArgumentException("The class " + klass.getName() + " does not have any methods annotated with @Profiled");
    }
    T proxyInstance = (T) Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class[]{klass},
            new ProfilingMethodInterceptor(delegate, clock, state)

    );
    return proxyInstance;

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

  }

  private <T> boolean profiledAnnotated(Class<T> klass) {
    boolean hasProfiledMethod = false;
    Method[] methods = klass.getDeclaredMethods();
    for(Method method : methods){
      if(method.isAnnotationPresent(Profiled.class)){
        hasProfiledMethod = true;
        break;
      }
    }

    return hasProfiledMethod;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    Writer writer = null;
    try {
      writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if(writer != null){
        try {
          writer.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
