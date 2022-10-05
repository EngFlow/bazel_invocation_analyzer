package com.engflow.bazel.invocation.analyzer.suggestionproviders;

import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;

public abstract class SuggestionProviderBase implements SuggestionProvider {
  protected SuggestionProviderUtil.SuggestionId createSuggestionId(String id) {
    return new SuggestionProviderUtil.SuggestionId(
        String.format("%s-%s", this.getClass().getName(), id));
  }
}
