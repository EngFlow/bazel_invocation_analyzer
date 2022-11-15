# Adding Suggestions to the Bazel Invocation Analyzer

This guide walks you through the steps to add new suggestions to the Bazel Invocation Analyzer by way of an example. This example covers most of the component types in the [Bazel Invocation Architecture](./library-infrastructure.md), with the notable exception of a provider that consumes external data (such as the Bazel Profile), which is an advanced topic. If you intend to create a provider that consumes a new data source, please study the [Bazel Profile](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile/BazelProfile.java) as a template.

You should be familiar with the [Bazel Invocation Architecture](./library-infrastructure.md). This will give you the big picture of how the various components fit together.

Note that this walk-through shows you the major aspects of the code. Some of the more mundane (but important) details such as error handling are left out for clarity. You can see these details in the actual classes the analyzer uses which are linked in the appropriate sections below.

## Getting Started

The first step to adding a suggestion is to identify a pattern in the data contained in Bazel profiles that indicates an opportunity for improvement. In this example, we will use the garbage collection data contained in Bazel profiles to provide a suggestion if the garbage collection events are taking longer than expected. The Bazel profile contains data about each garbage collection event, including timing:

![Bazel Profile viewed in the chrome://tracing tool](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/docs/assets/gc-profile-in-chrome-tracing.png?raw=true)

Bazel Profile viewed in the chrome://tracing tool

In this guide, we will create all the components necessary to extract the data and report on excessive Java garbage collection in Bazel.

## Datum

For our analysis, we need the total duration of major garbage collection events in the BazelProfile. We need to define a [Datum](./library-infrastructure.md#datum) to represent this value. Datums implement the [`Datum`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/Datum.java) interface and simply hold the data they represent:

```java
public class GarbageCollectionStats implements Datum {
  private final Duration majorGarbageCollectionDuration;

  public GarbageCollectionStats(Duration majorGarbageCollectionDuration) {
    this.majorGarbageCollectionDuration = majorGarbageCollectionDuration;
  }

  public Duration getMajorGarbageCollectionDuration() {
    return majorGarbageCollectionDuration;
  }

  // Interface methods...
}
```
*See [the actual implementation](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/GarbageCollectionStats.java) for more details.*

## DataProvider

Next, we'll need a [Data Provider](./library-infrastructure.md#data-provider) which extracts our value from the Bazel profile and makes our Datum available. Data Providers extend the [`DataProvider`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/DataProvider.java) abstract base class and implement the `getSuppliers()` function to register the `Datum` they provide:

```java
public class GarbageCollectionStatsDataProvider extends DataProvider {
  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            GarbageCollectionStats.class, memoized(this::getGarbageCollectionStats)));
  }
  
  public GarbageCollectionStats getGarbageCollectionStats() {...}
}
```

Here we're using a builder to create the `DatumSupplierSpecification` based on the Datum class we're providing, as well as using a class function to return the actual data. We're wrapping it in the optional `memoized` helper, which caches the resulting Datum so we don't recalculate it every time it gets requested by various other components. This is optional: If, for example, the Datum was a large stream of data we might not want it memoized.

To extract the data we're providing, we need to retrieve the Bazel profile from the [Data Manager](./library-infrastructure.md#data-manager) and extract the data we need from it:

```java
public GarbageCollectionStats getGarbageCollectionStats() {
  BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
  Optional<ProfileThread> garbageCollectorThread = bazelProfile.getGarbageCollectorThread();
  Duration majorGarbageCollection =
      garbageCollectorThread.get().getCompleteEvents().stream()
          .filter(
              event ->
                  BazelProfileConstants.COMPLETE_MAJOR_GARBAGE_COLLECTION.equals(event.name)
                      && BazelProfileConstants.CAT_GARBAGE_COLLECTION.equals(event.category))
          .map(event -> event.duration)
          .reduce(Duration.ZERO, Duration::plus);
  return new GarbageCollectionStats(majorGarbageCollection);
}
```

Here we're retrieving the events from the garbage collection thread in the profile, filtering them down to just the major garbage collections (by event name and category), and summing the durations. (*See [the actual implementation](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/GarbageCollectionStatsDataProvider.java) for complete details with error handling.*) 

Finally, we need to add our new [Data Provider](./library-infrastructure.md#data-provider) in [`DataProviderUtil.getAllDataProviders`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/DataProviderUtil.java#L32) so that it automatically gets registered with the `DataManager`:

```java
public class DataProviderUtil {
  public static List<DataProvider> getAllDataProviders() {
    return List.of(
        ...
        new GarbageCollectionStatsDataProvider());
  }
}
```

## SuggestionProvider

Now that we have the total major garbage collection time available, we can add the actual suggestion in a [Suggestion Provider](./library-infrastructure.md#suggestion-provider). Suggestion Providers extend the abstract [`SuggestionProviderBase`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderBase.java) class and implement the `getSuggestions` virtual function.

First, we'll retrieve the `GarbageCollectionStatsDataProvider` we created above from the [Data Manager](./library-infrastructure.md#data-manager) in the same way we retrieved the `BazelProfile`, check the data to see if it warrants a suggestion, and if so create the suggestion and return it. If the suggestion is not relevant, we simply return an empty list of suggestions:

```java
public class GCSuggestionProvider extends SuggestionProviderBase {
  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    GCStats gcStats = dataManager.getDatum(GCStats.class);
    if (!gcStats.majorGarbageCollection.isZero()) {
      Suggestion suggestion = null; // TODO: Use gcStats to create the suggestion
      return SuggestionProviderUtil.createSuggestionOutput(List.of(suggestion));
    }
    return SuggestionProviderUtil.createSuggestionOutput(List.of());
  }
}
```

We have a [Suggestion Provider Utility class](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderUtil.java) that helps us build suggestions and all the related fields. The `createSuggestion` helper takes the following elements of a suggestion:

- Title - short title for the suggestion
- Recommendation - this is the body of the recommendation itself, including details 
- Potential Improvement (optional) - how much faster could this invocation be if this suggestion is implemented
- Rationale (optional) - why this suggestion is being made based on the profile analyzed 
- Caveats (optional) - any stipulations about why this suggestion was made, or other information that could have been useful in validating or improving the suggestion 

```java
SuggestionProviderUtil.createSuggestion(
    "" /* title */,
    "" /* recommendation */,
    null /* potential improvement */,
    null /* rationale */,
    null /* caveats */);
```

For this example, we'll simply suggest increasing the Java heap size to give Bazel more memory to work with before garbage collection is necessary. We want to give as much detail as possible, including the Bazel flag (`--host_jvm_args`) that is used to adjust this size. We also want to give the rationale for why we're making this suggestion.

```java
String title = "Increase the Java heap size available to Bazel";

String recommendation =
    "Using the Bazel flag --host_jvm_args you can control the startup"
        + " options to be passed to the Java virtual machine in which Bazel"
        + " itself runs. You may want to increase the heap size."
        + "\nAlso see https://bazel.build/reference/command-line-reference"
        + "#flag--host_jvm_args";

String rationale =
    "Increasing the heap size may reduce the frequency and length of major"
        + " garbage collection.";
```

To provide information about the potential improvement, we want to compare the time in garbage collection to the total duration of the invocation to put it in perspective. Luckily there's already a [Datum](./library-infrastructure.md#datum) that contains this value; [`TotalDuration`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/TotalDuration.java). We don't need to know what [Data Provider](./library-infrastructure.md#data-provider) provides it, we just need to retrieve it from the [Data Manager](./library-infrastructure.md#data-manager):

```java
TotalDuration totalDuration = dataManager.getDatum(TotalDuration.class);
```

We can use another function in the [Suggestion Provider Utility class](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderUtil.java) to help us create the potential improvement. `createPotentialImprovement` takes the message we want to present as well as the potential reduction percentage. 

We'll also use functions in [Duration Utility class](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/time/DurationUtil.java) to both format the times we have, as well as to calculate the reduction percentage. 

```java
PotentialImprovement potentialImprovement =
    SuggestionProviderUtil.createPotentialImprovement(
        String.format(
            "Reducing the invocation's duration from %s to %s might be"
                + " possible. This assumes the stop-the-world pauses caused"
                + " by major garbage collection can be fully eliminated.",
            DurationUtil.format(totalDuration),
            DurationUtil.format(totalDuration.minus(gcStats.majorGc))),
        DurationUtil.getPercentageOf(gcStats.majorGc, totalDuration));
```

You can see this all come together, along with error handling and an additional suggestion about reducing memory usage in the actual [`GarbageCollectionSuggestionProvider`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/GarbageCollectionSuggestionProvider.java). 

Finally, we need to add our new [Suggestion Provider](./library-infrastructure.md#suggestion-provider) in [`SuggestionProviderUtil.getAllSuggestionProviders`](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderUtil.java#L43) so that it automatically gets applied to profile analysis requests:

```java
public static List<SuggestionProvider> getAllSuggestionProviders(boolean verbose) {
  return List.of(
      ...
      new GarbageCollectionSuggestionProvider());
}
```

We now have a working [Suggestion Provider](./library-infrastructure.md#suggestion-provider) utilizing the data from our [Data Provider](./library-infrastructure.md#data-provider)!

## Questions

If you still have questions, see potential improvements, or would like to provide feedback you can get in touch with us by:

- Emailing us at [analyzer@engflow.com](mailto:analyser@engflow.com)
- [Filing an issue on GitHub](https://github.com/EngFlow/bazel_invocation_analyzer/issues)
- Using the contact form on [www.engflow.com](https://www.engflow.com/contact?r=docs)
