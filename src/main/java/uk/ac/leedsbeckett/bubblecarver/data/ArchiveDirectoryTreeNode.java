/*
 * Copyright 2025 maber01.
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
package uk.ac.leedsbeckett.bubblecarver.data;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author maber01
 */
public class ArchiveDirectoryTreeNode extends DefaultMutableTreeNode
{
  private final static FileFilter fFilter = new FileFilter()
  {
    @Override
    public boolean accept(File file)
    {
      return file.isDirectory();
    }
  };  
  
  private final static Comparator<File> comparator = new Comparator<>()
  {
    @Override
    public int compare( File o1, File o2 )
    {
      return o1.compareTo( o2 );
    }
  };
  
  boolean hasArchivesDirectly   = false;
  boolean hasArchivesIndirectly = false;
  
  public ArchiveDirectoryTreeNode( File f )
  {
    FileData data = new FileData( f, f.getName() );
    setUserObject( data );
    if ( !f.isDirectory() )
      throw new IllegalArgumentException( "Cannot create node for non-directory file system item." );
    // unpacked archive directory will have .zip ending
    if ( f.getName().endsWith( ".zip" ) )
    {
      setAllowsChildren( false );
    }
    else
    {
      // Add children recursively
      setAllowsChildren( true );
      File[] childrenFiles = f.listFiles( fFilter );
      Arrays.sort( childrenFiles, comparator );
      for ( File child : childrenFiles )
      {
        ArchiveDirectoryTreeNode childnode = new ArchiveDirectoryTreeNode( child );
        if ( child.getName().endsWith( ".zip" ) )
        {
          hasArchivesDirectly = true;
          add( childnode );
        }
        else if ( childnode.hasArchivesDirectly || childnode.hasArchivesIndirectly )
        {
          hasArchivesIndirectly = true;
          add( childnode );
        }
      }
    }
  }
  
  public FileData getFileData()
  {
    return (FileData)getUserObject();
  }
}
