/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.auto.javaconcurrent;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.auto.api.ContextStore;
import io.opentelemetry.instrumentation.auto.api.InstrumentationContext;
import io.opentelemetry.instrumentation.auto.api.concurrent.AdviceUtils;
import io.opentelemetry.instrumentation.auto.api.concurrent.State;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.Map;
import java.util.concurrent.Callable;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/** Instrument {@link Runnable} and {@link Callable} */
@AutoService(Instrumenter.class)
public final class CallableInstrumentation extends Instrumenter.Default {

  public CallableInstrumentation() {
    super(AbstractExecutorInstrumentation.EXEC_NAME);
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named(Callable.class.getName()));
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap(Callable.class.getName(), State.class.getName());
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("call").and(takesArguments(0)).and(isPublic()),
        CallableInstrumentation.class.getName() + "$CallableAdvice");
  }

  public static class CallableAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope enter(@Advice.This Callable thiz) {
      ContextStore<Callable, State> contextStore =
          InstrumentationContext.get(Callable.class, State.class);
      return AdviceUtils.startTaskScope(contextStore, thiz);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(@Advice.Enter Scope scope) {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
