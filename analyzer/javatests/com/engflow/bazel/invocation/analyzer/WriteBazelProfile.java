/*
 * Copyright 2022 EngFlow Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.engflow.bazel.invocation.analyzer;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.Property.put;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.newOutputStream;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.TraceEventFormatConstants;
import com.google.common.collect.ObjectArrays;
import com.google.gson.stream.JsonWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Defines an embedded domain specific language class to simplify writing Bazel profile events.
 *
 * <p>Usage:
 *
 * <pre>
 *   var profile = WriteBazelProfile.to(
 *       profilePath,
 *       metaData(put("build id", "42")),
 *       trace(thread(1, 0, "Critical Path", complete("CPP", "critical path component", timestamp, 10))
 *   );
 * </pre>
 */
public class WriteBazelProfile {
  public static InputStream toInputStream(ProfileSection... events) {
    StringWriter writer = new StringWriter();
    writeEvents(writer, events);
    return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** The entry point to the bazel profile dsl that write gzipped json. */
  public static Path toCompressed(Path file, ProfileSection... events) {
    try (var compressing =
            new GZIPOutputStream(newOutputStream(file, StandardOpenOption.CREATE_NEW));
        var fileWriter = new OutputStreamWriter(compressing, StandardCharsets.UTF_8)) {
      writeEvents(fileWriter, events);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return file;
  }

  /** The entry point to the bazel profile dsl. */
  public static Path to(Path file, ProfileSection... events) {
    try (var fileWriter = newBufferedWriter(file, StandardOpenOption.CREATE_NEW)) {
      writeEvents(fileWriter, events);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return file;
  }

  /**
   * Create a new main thread for the {@link #trace(TraceEvent...)}.
   *
   * @param events a series of events that belong to the thread
   * @return an instance of {@link TraceEvent} to be serialized to json.
   */
  @CheckReturnValue
  public static TraceEvent mainThread(ThreadEvent... events) {
    return thread(-1, 0, BazelProfileConstants.THREAD_MAIN, events);
  }

  /**
   * Create a new thread for the {@link #trace(TraceEvent...)}.
   *
   * @param id for the thread
   * @param index of the thread
   * @param name of the thread
   * @param events a series of events that belong to the thread
   * @return an instance of {@link TraceEvent} to be serialized to json.
   */
  @CheckReturnValue
  public static TraceEvent thread(
      @Nullable Integer id, int index, String name, ThreadEvent... events) {
    return out -> {
      if (id != null) {
        out.object(
            put(
                TraceEventFormatConstants.EVENT_NAME,
                TraceEventFormatConstants.METADATA_THREAD_NAME),
            put(TraceEventFormatConstants.EVENT_PHASE, TraceEventFormatConstants.PHASE_METADATA),
            put(TraceEventFormatConstants.EVENT_THREAD_ID, id),
            put(TraceEventFormatConstants.EVENT_PROCESS_ID, 1),
            put(TraceEventFormatConstants.EVENT_ARGUMENTS, put("name", name)));
        out.object(
            put(
                TraceEventFormatConstants.EVENT_NAME,
                TraceEventFormatConstants.METADATA_THREAD_SORT_INDEX),
            put(TraceEventFormatConstants.EVENT_PHASE, TraceEventFormatConstants.PHASE_METADATA),
            put(TraceEventFormatConstants.EVENT_THREAD_ID, id),
            put(TraceEventFormatConstants.EVENT_PROCESS_ID, 1),
            put(TraceEventFormatConstants.EVENT_ARGUMENTS, put("sort_index", index)));
      }

      for (ThreadEvent event : events) {
        final AtomicBoolean topLevel = new AtomicBoolean(true);
        var threadWriter =
            new BaseJsonWriter(out) {
              @Override
              public void object(Contents contents) {
                out.object(
                    () -> {
                      // ensures tid and pid are only added to the events but not nested
                      // objects like 'args'
                      if (topLevel.getAndSet(false)) {
                        if (id != null) {
                          put(TraceEventFormatConstants.EVENT_THREAD_ID, id);
                        }
                        put(TraceEventFormatConstants.EVENT_PROCESS_ID, 1);
                      }
                      contents.apply();
                    });
              }
            };
        event.writeTo(threadWriter);
      }
    };
  }

  /**
   * Create a new "count" event for a {@link TraceEvent}
   *
   * @param name of the count
   * @param timestamp when the count is recorded
   * @param type of the metric counted (common types: actions, memory, etc.)
   * @return an instance of {@link ThreadEvent} that belongs to {@link TraceEvent}.
   */
  @CheckReturnValue
  public static ThreadEvent count(String name, long timestamp, String type, String count) {
    return writer ->
        writer.object(
            put(TraceEventFormatConstants.EVENT_NAME, name),
            put(TraceEventFormatConstants.EVENT_PHASE, TraceEventFormatConstants.PHASE_COUNTER),
            put(TraceEventFormatConstants.EVENT_TIMESTAMP, timestamp),
            put(TraceEventFormatConstants.EVENT_ARGUMENTS, put(type, count)));
  }

  /**
   * An "instant" event, indicating change in a {@link TraceEvent}.
   *
   * @param name of the event
   * @param category of the event
   * @param timestamp of the event
   * @return an instance of {@link ThreadEvent} that belongs to {@link TraceEvent}.
   */
  @CheckReturnValue
  public static ThreadEvent instant(String name, String category, Timestamp timestamp) {
    return writer ->
        writer.object(
            put(TraceEventFormatConstants.EVENT_NAME, name),
            put(TraceEventFormatConstants.EVENT_CATEGORY, category),
            put(TraceEventFormatConstants.EVENT_TIMESTAMP, timestamp.getMicros()),
            put(TraceEventFormatConstants.EVENT_PHASE, TraceEventFormatConstants.PHASE_INSTANT));
  }

  /**
   * Utility method to simplify programmatic event creation.
   *
   * <p>Usage:
   *
   * <pre>
   *   thread(42, 67, "Critical Path",
   *     sequence(
   *       Stream.of(10, 20, 30),
   *       timestamp -> complete("mnemonic", "critical path component", timestamp, duration)
   *     )
   *   )
   * </pre>
   *
   * @param items a stream of changing values to generate an event.
   * @param generator function to create {@link ThreadEvent} instances.
   * @return an array of {@link ThreadEvent}s to pass to {@link #thread(Integer, int, String,
   *     ThreadEvent...)}
   */
  @CheckReturnValue
  public static <T> ThreadEvent[] sequence(Stream<T> items, Function<T, ThreadEvent> generator) {
    return items.map(generator).toArray(ThreadEvent[]::new);
  }

  /**
   * Join 2 or more arrays of {@link ThreadEvent}. Particularly useful when a profile has more than
   * a sequence of events or counters.
   *
   * <p>Usage
   *
   * <p>
   *
   * <pre>
   *   concat(
   *    sequence(Stream.of(0, 1, 1, 2, 3, 5), i -> count("fib", 0, "fib", i.toString())),
   *    sequence(Stream.of(10, 20, 30), timestamp -> complete("mnemonic", "critical path component", timestamp, duration))
   *  )
   * </pre>
   *
   * @param events array of arrays of {@link ThreadEvent} to concatenate in a single array of {@link
   *     ThreadEvent}.
   */
  @CheckReturnValue
  public static ThreadEvent[] concat(ThreadEvent[]... events) {
    ThreadEvent[] collector = new ThreadEvent[0];
    for (ThreadEvent[] threadEvents : events) {
      collector = ObjectArrays.concat(collector, threadEvents, ThreadEvent.class);
    }
    return collector;
  }

  /**
   * A "complete" event, measuring the duration in a profile {@link #trace(TraceEvent...)}
   *
   * @param name of the event
   * @param category of the event
   * @param timestamp when the event occurred.
   * @param duration of how long the event took.
   * @return an instance of {@link ThreadEvent} that belongs to {@link TraceEvent}.
   */
  @CheckReturnValue
  public static ThreadEvent complete(
      String name,
      String category,
      Timestamp timestamp,
      Duration duration,
      Property... properties) {
    return writer ->
        writer.object(
            put(TraceEventFormatConstants.EVENT_NAME, name),
            put(TraceEventFormatConstants.EVENT_CATEGORY, category),
            put(TraceEventFormatConstants.EVENT_TIMESTAMP, timestamp.getMicros()),
            put(TraceEventFormatConstants.EVENT_DURATION, TimeUtil.getMicros(duration)),
            put(TraceEventFormatConstants.EVENT_PHASE, TraceEventFormatConstants.PHASE_COMPLETE),
            put(TraceEventFormatConstants.EVENT_ARGUMENTS, properties));
  }

  /**
   * A mnemonic to add to the args.
   *
   * @param name for the event
   */
  @CheckReturnValue
  public static Property mnemonic(String name) {
    return writer -> writer.put("mnemonic", name);
  }

  /**
   * The "otherData" section of a profile, containing information about the profile.
   *
   * <p>See {@link Property#put(String, String)}, though the consumption of the properties in
   * "otherData" does not appear to be structured.
   *
   * @param properties a series of string to string pairs of profile metadata.
   * @return {@link ProfileSection} for {@link WriteBazelProfile#to(Path, ProfileSection...)}
   */
  public static ProfileSection metaData(Property... properties) {
    return json -> json.object(TraceEventFormatConstants.SECTION_OTHER_DATA, properties);
  }

  /**
   * The "traceEvents" section of a json profile.
   *
   * @param events events in the profile tracing.
   * @return {@link ProfileSection} for {@link WriteBazelProfile#to(Path, ProfileSection...)}
   */
  public static ProfileSection trace(TraceEvent... events) {
    return json ->
        json.put(
            TraceEventFormatConstants.SECTION_TRACE_EVENTS,
            () ->
                json.array(
                    () -> {
                      for (var event : events) {
                        event.writeTo(json);
                      }
                    }));
  }

  /** Marker interface for the section of the profile. */
  public interface ProfileSection extends WriteJson {}

  /** Marker interface for events in the {@link #trace(TraceEvent...)} {@link ProfileSection}. */
  public interface TraceEvent extends WriteJson {}

  /**
   * Marker interface for events in a {@link #thread(Integer, int, String, ThreadEvent...)} event.
   */
  public interface ThreadEvent extends WriteJson {}

  /** Property represents a key/value pair in a json object. */
  public interface Property extends WriteJson {

    /** writes a String key and String value into a json object. */
    @CheckReturnValue
    static Property put(String name, String value) {
      return writer -> writer.put(name, value);
    }

    /** writes a String key and long value into a json object. */
    @CheckReturnValue
    static Property put(String name, long value) {
      return writer -> writer.put(name, value);
    }

    /** writes a String key and object value containing the properties into a json object. */
    @CheckReturnValue
    static Property put(String name, Property... properties) {
      if (properties.length == 0) {
        return writer -> {};
      }
      return writer -> writer.put(name, () -> writer.object(properties));
    }
  }

  /* Implementation interfaces and classes to support the dsl. */

  private static void writeEvents(Writer fileWriter, ProfileSection... events) {
    try (var json = new BaseJsonWriter(new JsonWriter(fileWriter))) {
      json.object(
          () -> {
            for (var event : events) {
              event.writeTo(json);
            }
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private interface WriteJson {
    void writeTo(SimpleJsonWriter out);
  }

  private interface SimpleJsonWriter extends Closeable {

    void json(UncheckWrite block);

    default void object(String name, Contents contents) {
      put(name, () -> object(contents));
    }

    default void object(Contents contents) {
      json(
          writer -> {
            writer.beginObject();
            contents.apply();
            writer.endObject();
          });
    }

    default void object(String name, Property... properties) {
      put(name, () -> object(properties));
    }

    default void object(Property... properties) {
      object(
          () -> {
            for (var property : properties) {
              property.writeTo(this);
            }
          });
    }

    default void put(String name, Contents contents) {
      json(
          writer -> {
            writer.name(name);
            contents.apply();
          });
    }

    default void put(String name, long value) {
      put(name, () -> json(writer -> writer.value(value)));
    }

    default void put(String name, String value) {
      put(name, () -> json(writer -> writer.value(value)));
    }

    default void array(Contents contents) {
      json(
          writer -> {
            writer.beginArray();
            contents.apply();
            writer.endArray();
          });
    }

    interface UncheckWrite {
      void writeTo(JsonWriter writer) throws IOException;
    }

    interface Contents {
      void apply() throws IOException;
    }

    @Override
    default void close() throws IOException {}
  }

  private static class BaseJsonWriter implements SimpleJsonWriter, Closeable {
    private final SimpleJsonWriter writer;

    BaseJsonWriter(JsonWriter writer) {
      this(
          new SimpleJsonWriter() {
            @Override
            public void json(UncheckWrite block) {
              try {
                block.writeTo(writer);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }

            @Override
            public void close() throws IOException {
              writer.flush();
              writer.close();
            }
          });
    }

    BaseJsonWriter(SimpleJsonWriter writer) {
      this.writer = writer;
    }

    @Override
    public void json(UncheckWrite block) {
      writer.json(block);
    }

    @Override
    public void close() throws IOException {
      writer.close();
    }
  }
}
