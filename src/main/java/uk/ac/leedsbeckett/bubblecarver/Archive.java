/*
 * Copyright 2025 maber01.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this zipdir except in compliance with the License.
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
package uk.ac.leedsbeckett.bubblecarver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.leedsbeckett.bubblecarver.data.ElementTreeData;

/**
 *
 * @author maber01
 */
public class Archive
{
  public static final String NS_BB_PACKAGE = "http://www.blackboard.com/content-packaging/";
  
  DocumentBuilder builder;
  XPath xPath = XPathFactory.newInstance().newXPath();

  Path zipdir;
  Document manifestDocument;
  TreeModel manifestTreeModel;
  Document courseSettingsDocument;
  TreeModel courseSettingsTreeModel;
  Document usersDocument;
  TreeModel usersTreeModel;
  Document membershipDocument;
  TreeModel membershipTreeModel;
  Document gradebookDocument;
  TreeModel gradebookTreeModel;
  
  ArrayList<Resource> attemptFilesResource   = new ArrayList<>();
  ArrayList<TreeModel> attemptFilesTreeModel = new ArrayList<>();

  ArrayList<Element> outdefList = new ArrayList<>();
  
  public Archive( Path path )
  {
    this.zipdir = path;
  }

  public Document getManifestDocument()
  {
    return manifestDocument;
  }
  
  public void load() throws ArchiveException
  {
    Path manifestpath;
    manifestTreeModel = null;
    courseSettingsTreeModel = null;
    usersTreeModel = null;
    membershipTreeModel = null;
    gradebookTreeModel = null;
    attemptFilesTreeModel.clear();
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware( true );
      builder = factory.newDocumentBuilder();
      manifestpath = zipdir.resolve( "imsmanifest.xml" );
      if ( !Files.exists( manifestpath ) )
        throw new ArchiveException( "The ZIP file does not contain an IMS manifest and so doesn't look like a BBL archive." );
      manifestDocument = loadXMLDocument( manifestpath );
      courseSettingsDocument = loadXMLDocument( findCourseSettings() );
      usersDocument = loadXMLDocument( findUsers() );
      membershipDocument = loadXMLDocument( findMembership() );
      gradebookDocument = loadXMLDocument( findGradebook() );
      attemptFilesResource = findAttemptFilesList();
      for ( Resource r : attemptFilesResource )
        r.setDocument( loadXMLDocument( r ) );
    }
    catch ( IOException | ParserConfigurationException | SAXException | XPathExpressionException ex )
    {
      Logger.getLogger( Archive.class.getName() ).log( Level.SEVERE, null, ex );
      throw new ArchiveException( "IO problem loading archive: " + ex.getMessage(), ex );
    }
  }
  
  public void loadTrees()
  {
    manifestTreeModel = loadTreeModel( manifestDocument );
    if ( courseSettingsDocument != null )
      courseSettingsTreeModel = loadTreeModel( courseSettingsDocument );
    if ( usersDocument != null )
      usersTreeModel = loadTreeModel( usersDocument );
    if ( membershipDocument != null )
      membershipTreeModel = loadTreeModel( membershipDocument );
    if ( gradebookDocument != null )
      gradebookTreeModel = loadTreeModel( gradebookDocument );
    for ( Resource r : attemptFilesResource )
      attemptFilesTreeModel.add( loadTreeModel( r.getDocument() ) );    
  }

  public Resource findCourseSettings() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'course/x-bb-coursesetting']", false );
  }

  public Resource findUsers() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'course/x-bb-user']", false );
  }

  public Resource findMembership() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'membership/x-bb-coursemembership']", false );
  }
  
  public Resource findGradebook() throws XPathExpressionException, ArchiveException
  {
    return findEntry( "/manifest/resources/resource[@type = 'course/x-bb-gradebook']", false );
  }
  
  public ArrayList<Resource> findAttemptFilesList() throws XPathExpressionException, ArchiveException
  {
    return findEntries( "/manifest/resources/resource[@type = 'course/x-bb-attemptfiles']" );
  }
  
  public Resource findEntry( String expression, boolean needed ) throws XPathExpressionException, ArchiveException
  {
    NodeList nodeList = xPathNodeList( expression, manifestDocument );
    if ( !needed && nodeList.getLength() == 0 )
      return null;
    if ( nodeList.getLength() == 0 )
      throw new ArchiveException( "Archive doesn't contain data file that matches " + expression + "." );
    Element resource = (Element)nodeList.item( 0 );
    return findEntry( resource );
  }

  public Resource findEntry( Element resource ) throws ArchiveException
  {
    String filename = resource.getAttributeNS( NS_BB_PACKAGE, "file" );
    if ( filename == null || filename.trim().length() == 0 )
      throw new ArchiveException( "Archive's entry does not have a filename." );
    Path path = this.zipdir.resolve( filename );
    if ( !Files.exists( path ) )
      throw new ArchiveException( "Archive's entry named " + filename + " does not exist in unzipped archive." );
    return new Resource( 
            resource.getAttributeNS( "http://www.w3.org/XML/1998/namespace", "base" ), 
            path );
  }

  public ArrayList<Resource> findEntries( String expression ) throws XPathExpressionException, ArchiveException
  {
    ArrayList<Resource> list = new ArrayList<>();
    NodeList nodeList = (NodeList) xPath.compile( expression ).evaluate( manifestDocument, XPathConstants.NODESET);
    for ( int i=0; i<nodeList.getLength(); i++ )
    {
      Element resource = (Element)nodeList.item( i );
      String filename = resource.getAttributeNS( NS_BB_PACKAGE, "file" );
      if ( filename == null || filename.trim().length() == 0 )
        throw new ArchiveException( "Archive's entry that matches " + expression + " does not have a filename." );
      Path path = this.zipdir.resolve( filename );
      if ( !Files.exists( path ) )
        throw new ArchiveException( "Archive's entry named " + filename + " does not exist in unzipped archive." );
      list.add( 
          new Resource( resource.getAttributeNS( "http://www.w3.org/XML/1998/namespace", "base" ), 
          path ) );
    }
    return list;
  }

  
  public TreeModel getManifestTreeModel()
  {
    return manifestTreeModel;
  }

  public TreeModel getCourseSettingsTreeModel()
  {
    return courseSettingsTreeModel;
  }

  public TreeModel getUsersTreeModel()
  {
    return usersTreeModel;
  }

  public TreeModel getMembershipTreeModel()
  {
    return membershipTreeModel;
  }  

  public TreeModel getGradebookTreeModel()
  {
    return gradebookTreeModel;
  }

  public List<TreeModel> getAttemptFilesTreeModel()
  {
    return attemptFilesTreeModel;
  }
  
  
  public Document loadXMLDocument( Resource resource ) throws IOException, SAXException
  {
    if ( resource == null ) return null;
    if ( resource.getPath() == null ) return null;
    return loadXMLDocument( resource.getPath() );
  }
  
  Document loadXMLDocument( Path path ) throws IOException, SAXException
  {
    try ( InputStream in = Files.newInputStream( path ) )
    {
      Document doc = builder.parse( in );
      doc.getDocumentElement().normalize();
      return doc;
    }
  }

  
  
  public TreeModel loadTreeModel( Document doc )
  {
    return new DefaultTreeModel( loadTreeModelNode( doc.getDocumentElement() ) );
  }
  
  DefaultMutableTreeNode loadTreeModelNode( Element e )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "<" );
    if ( e.getPrefix() != null && e.getPrefix().length() > 0 )
    {
      sb.append( e.getPrefix() );
      sb.append( ":" );
    }
    sb.append( e.getLocalName() );
    NamedNodeMap atts = e.getAttributes();
    for ( int i=0; i<atts.getLength(); i++ )
    {
      sb.append( " " );
      Attr a = (Attr)atts.item( i );
      if ( a.getPrefix() != null && a.getPrefix().length() > 0 )
      {
        sb.append( a.getPrefix() );
        sb.append( ":" );
      }
      sb.append( a.getLocalName() );
      sb.append( "=\"" );
      sb.append( a.getValue() );
      sb.append( "\"" );
    }
    sb.append( ">" );
    
    DefaultMutableTreeNode node = new DefaultMutableTreeNode( new ElementTreeData( e, sb.toString() ) );
    NodeList children = e.getChildNodes();
    if ( children.getLength() == 1 &&
         ( children.item( 0 ).getNodeType() == Node.TEXT_NODE ||
           children.item( 0 ).getNodeType() == Node.CDATA_SECTION_NODE ) )
    {
      String t = children.item( 0 ).getTextContent();
      if ( t != null && t.trim().length() != 0 )
      {
        node.add( new DefaultMutableTreeNode( t ) );
        return node;
      }
    }
    for ( int i=0; i<children.getLength(); i++ )
    {
      if ( children.item( i ) instanceof Element )
      {
        Element child = (Element)children.item( i );
        node.add( loadTreeModelNode( child ) );
      }
    }

    return node;
  }

  private String xPathString( String expr, Object node ) throws XPathExpressionException
  {
      return (String)xPath.compile( expr ).evaluate( node, XPathConstants.STRING );
  }

  private NodeList xPathNodeList( String expr, Object node ) throws XPathExpressionException
  {
      return (NodeList)xPath.compile( expr ).evaluate( node, XPathConstants.NODESET );
  }
  
  
  public class Resource
  {
    private final String base;
    private final Path path;
    private Document document = null;

    public Resource( String base, Path path )
    {
      this.base = base;
      this.path = path;
    }

    public String getBase()
    {
      return base;
    }

    public Path getPath()
    {
      return path;
    }

    public Document getDocument()
    {
      return document;
    }

    public void setDocument( Document document )
    {
      this.document = document;
    }
  }  
}
