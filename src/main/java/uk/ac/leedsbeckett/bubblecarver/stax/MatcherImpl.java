/*
 * Copyright 2026 maber01.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.leedsbeckett.bubblecarver.stax;

import java.util.function.BiConsumer;

/**
 *
 * @author maber01
 */
class MatcherImpl implements Matcher {
  
  final String[] modes;
  final String[] xpaths;
  final BiConsumer<MatchProcessor, ElementContext> consumer;

  public MatcherImpl(String[] modes, String[] xpaths, BiConsumer<MatchProcessor, ElementContext> consumer) {
    this.modes = modes;
    this.xpaths = xpaths;
    this.consumer = consumer;
  }

  public MatcherImpl(String mode, String xpath, BiConsumer<MatchProcessor, ElementContext> consumer) {
    this.modes = new String[]{mode};
    this.xpaths = new String[]{xpath};
    this.consumer = consumer;
  }

  public MatcherImpl(StudentRedactor.Mode mode, String xpath, BiConsumer<MatchProcessor, ElementContext> consumer) {
    this.modes = new String[]{mode.getName()};
    this.xpaths = new String[]{xpath};
    this.consumer = consumer;
  }

  public MatcherImpl(String xpath, BiConsumer<MatchProcessor, ElementContext> consumer) {
    this.modes = new String[]{};
    this.xpaths = new String[]{xpath};
    this.consumer = consumer;
  }

  public MatcherImpl(StudentRedactor.Mode mode, BiConsumer<MatchProcessor, ElementContext> consumer) {
    this.modes = new String[]{mode.getName()};
    this.xpaths = new String[]{};
    this.consumer = consumer;
  }

  public MatcherImpl(BiConsumer<MatchProcessor, ElementContext> consumer) {
    this.modes = new String[]{};
    this.xpaths = new String[]{};
    this.consumer = consumer;
  }

  @Override
  public String[] getModes() {
    return modes;
  }

  @Override
  public String[] getXPaths() {
    return xpaths;
  }

  @Override
  public BiConsumer<MatchProcessor, ElementContext> getConsumer() {
    return consumer;
  }
  
}
