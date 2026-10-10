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
package uk.ac.leedsbeckett.bubblecarver.combiner;

import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import uk.ac.leedsbeckett.bubblecarver.data.GeneralMap;
import uk.ac.leedsbeckett.bubblecarver.staxfilter.FilterContext;

/**
 *
 * @author maber01
 */
public class BubbleCombinerContext extends FilterContext
{
  final Path inputBasePath;
  final Path inputPath;
  final Path outputPath;
  
  final XMLOutputFactory writeFactory = XMLOutputFactory.newInstance();
  final XMLInputFactory readerFactory = XMLInputFactory.newInstance();
  
  private final GeneralMap generalMap = new GeneralMap();
  
  long nextBcarvId = 1;

  public BubbleCombinerContext( Path inputPath, Path outputPath )
  {
    this.inputBasePath = inputPath.getParent();
    this.inputPath     = inputPath;
    this.outputPath    = outputPath;
  }

  public Path getInputBasePath() {
    return inputBasePath;
  }

  public Path getInputPath()
  {
    return inputPath;
  }

  public Path getOutputPath()
  {
    return outputPath;
  }

  public XMLOutputFactory getWriteFactory() {
    return writeFactory;
  }

  public XMLInputFactory getReaderFactory() {
    return readerFactory;
  }

  public GeneralMap getGeneralMap()
  {
    return generalMap;
  }
  
  public String getNextBackreferenceId()
  {
    return Long.toHexString( nextBcarvId++ );
  }
}
