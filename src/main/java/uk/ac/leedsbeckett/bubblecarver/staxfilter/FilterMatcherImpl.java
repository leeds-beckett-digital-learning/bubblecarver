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
package uk.ac.leedsbeckett.bubblecarver.staxfilter;


/**
 *
 * @author maber01
 */
public class FilterMatcherImpl implements FilterMatcher {
  
  final String[] modes;
  final String[] xpaths;
  final StartElementHandler handler;

  public FilterMatcherImpl(String[] modes, String[] xpaths, StartElementHandler handler) {
    this.modes = modes;
    this.xpaths = xpaths;
    this.handler = handler;
  }

  public FilterMatcherImpl(String mode, String xpath, StartElementHandler handler) {
    this.modes = new String[]{mode};
    this.xpaths = new String[]{xpath};
    this.handler = handler;
  }

  public FilterMatcherImpl(FilterMode mode, String xpath, StartElementHandler handler) {
    this.modes = new String[]{mode.getName()};
    this.xpaths = new String[]{xpath};
    this.handler = handler;
  }

  public FilterMatcherImpl(String xpath, StartElementHandler handler) {
    this.modes = new String[]{};
    this.xpaths = new String[]{xpath};
    this.handler = handler;
  }

  public FilterMatcherImpl(FilterMode mode, StartElementHandler handler) {
    this.modes = new String[]{mode.getName()};
    this.xpaths = new String[]{};
    this.handler = handler;
  }

  public FilterMatcherImpl(StartElementHandler handler) {
    this.modes = new String[]{};
    this.xpaths = new String[]{};
    this.handler = handler;
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
  public StartElementHandler getHandler()
  {
    return handler;
  }
  
}
