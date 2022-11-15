# Bazel Invocation Analyzer Architecture

The Bazel Invocation Analyzer consumes input data about a Bazel build or test and produces suggestions on how to improve that specific invocation. It does this by mapping input data into intermediate data which is eventually consumed and turned into one or more suggestions.

## Logical System Design Overview

<img src="https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/docs/assets/library-architecture.svg" />

To facilitate the transformation of data into suggestions there are a few key classes that form a pipeline. [Datum suppliers](#datum-supplier) consume pieces of data and supply one or more pieces of data. [Suggestion providers](#suggestion-provider) consume the resulting data and produce one or more suggestions specific to that invocation.

Much of the data produced by datum suppliers is used by multiple downstream objects. Instead of fetching the data directly from the datum supplier that supplies it the [data manager](#data-manager) serves as an intermediate from which all data can be fetched. Furthermore, [data providers](#data-providers) group data which may be cheap to calculate simultaneously such that multiple data may be calculated in one pass. Having data tracked by the data manager allows the system to trace what data is used to provide a certain suggestion. Likewise, suggestion providers must also fetch the data used from the data manager.

The Bazel Invocation Analyzer is architected in such a way that one may introduce new datum suppliers that make use of the exiting Bazel profile data and/or data from other datum suppliers, or one may introduce datum suppliers that rely on information not yet in the system such as data from BEP streams or other sources.


## Datum

Defined in [analyzer/java/com/engflow/bazel/invocation/analyzer/core/Datum.java](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/Datum.java)

A Datum is a single piece of data that is produced by a [datum supplier](#datum-supplier). Suppliers for its class type are registered on the [data manager](#data-manager) from which it can be created and retrieved.


## Data Provider

Defined in [analyzer/java/com/engflow/bazel/invocation/analyzer/core/DatumProvider.java](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/DatumProvider.java)

Data Providers group multiple [datum suppliers](#datum-supplier) that may be logically related. They may hold state that is exposed to multiple datum suppliers such that data can be internally cached, provide shared logic to multiple suppliers, or group datum that is just more efficient to collect at one time.

Data Providers must register themselves with the [data manager](#data-manager). This performs two functions: First, it registers all of the supplied [datum](#datum) types with the data manager. Second, it stores the data manager that it was registered with such that it can retrieve datum from other datum suppliers via the data manager.


## Datum Supplier

Defined in [analyzer/java/com/engflow/bazel/invocation/analyzer/core/DatumSupplier.java](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/DatumSupplier.java)

Datum suppliers produce a specific type of [datum](#datum). They may get their input directly (such as with the [BazelProfile](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile/BazelProfile.java) datum supplier), but more often they fetch data from the [data manager](#data-manager).

All datum suppliers should be managed by a [data provider](#datum-provider). This will expose a data manager for these suppliers to use.

These are often just exposed as a list of functions from a data provider.


## Suggestion Provider

Defined in [analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderBase.java](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderBase.java)

Suggestion providers fetch [data](#datum) from the [data manager](#data-manager) and produce a [suggestion output](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/proto/bazel_invocation_analyzer.proto). This suggestion output may include none or many suggestions, caveats about the suggestions or the execution overall, missing inputs that may improve the provided suggestions, and, if a severe failure occurred, details about why the suggestion provider could not continue its analysis.


## Data Manager


Defined in [analyzer/java/com/engflow/bazel/invocation/analyzer/core/DataManager.java](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/DataManager.java)

The data manager is the core of the system. It tracks all [datum suppliers](#datum-supplier) registered by [data providers](#data-provider). When a specific [datum](#datum) is requested it requests that datum from the datum supplier (which may generate the data or return a memoized version). It also tracks which datum has been requested for informational or debugging purposes.

Datum suppliers and [suggestion providers](#suggestion-provider) should avoid using any inputs not provided by the data manager.
