<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : studentreport.xsl
    Created on : 8 January 2026, 10:47
    Author     : maber01
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:bbcarv="http://www.leedsbeckett.ac.uk/bubblecarver"
  xmlns:java="http://xml.apache.org/xalan/java"
  extension-element-prefixes="java"
  version="1.0">
  <xsl:output method="html"/>

  <!-- TODO customize transformation rules 
       syntax recommendation http://www.w3.org/TR/xslt 
  -->
  <xsl:template match="/">
    <html>
      <head>
        <title>studentreport.xsl</title>
        <link rel="stylesheet" href="../../../../../../css/report.css" />
      </head>
      <body>
        <h1><xsl:value-of select="bbcarv:student/@sid"/></h1>
        <h2>
          <xsl:value-of select=".//USERS/USER/NAMES/FAMILY/@value"/>,
          <xsl:value-of select=".//USERS/USER/NAMES/GIVEN/@value"/> 
        </h2>
        <h1>Grade Centre Data</h1>
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Points Avail.</th>
              <th>Sub/Feedback/Notes</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates mode="tabular" select="bbcarv:student/bbcarv:resources/bbcarv:resource/bbcarv:data/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION"/>
          </tbody>
        </table>
        <h1>More Detailed Grade Centre Data</h1>
        <xsl:apply-templates select="bbcarv:student/bbcarv:resources/bbcarv:resource/bbcarv:data/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="OUTCOMEDEFINITION" mode="tabular">
    <xsl:variable name="title">
      <xsl:choose>
        <xsl:when test="TITLE/@value = 'OutcomeDefinition.Total.title'">Total</xsl:when>
        <xsl:when test="TITLE/@value = 'OutcomeDefinition.WeightedTotal.title'">Weighted Total</xsl:when>
        <xsl:otherwise><xsl:value-of select="TITLE/@value"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <tr>
      <th><xsl:value-of select="$title"/></th>
      <td><xsl:value-of select="POINTSPOSSIBLE/@value"/></td>
      <td>
        <xsl:if test="count( OUTCOMES/OUTCOME ) = 0">
          <p>No outcomes recorded.</p>
        </xsl:if>
        <xsl:apply-templates select="OUTCOMES/OUTCOME"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="OUTCOMEDEFINITION">
    <h2><xsl:value-of select="TITLE/@value"/></h2>
    <p>Points possible = <xsl:value-of select="POINTSPOSSIBLE/@value"/></p>
    <xsl:if test="count( OUTCOMES/OUTCOME ) = 0">
      <p>No outcomes recorded.</p>
    </xsl:if>
    <xsl:apply-templates select="OUTCOMES/OUTCOME"/>
  </xsl:template>

  <xsl:template match="OUTCOME">
    <xsl:apply-templates select="ATTEMPTS/ATTEMPT"/>
  </xsl:template>
  
  <xsl:template match="ATTEMPT">
    <xsl:variable name="attemptid" select="@id"/>
    <h2>Attempt <xsl:value-of select="@id"/></h2>
    <div class="attempt-submission">
      <h3>Textual Submission</h3>
      <div>
        <xsl:apply-templates select="STUDENTSUBMISSION"/>
      </div>
      <h3>Attached Files</h3>
      <ul>
        <xsl:apply-templates select="/bbcarv:student/bbcarv:resources/bbcarv:resource[@type = 'course/x-bb-attemptfiles']/bbcarv:data/ATTEMPTFILES/ATTEMPTFILE[ATTEMPTID/@value = $attemptid]"/>
      </ul>
    </div>
    <div class="attempt-comments">
      <xsl:apply-templates select="INSTRUCTORCOMMENTS"/>
    </div>
    <div class="attempt-notes">
      <xsl:apply-templates select="INSTRUCTORNOTES"/>
    </div>
  </xsl:template>

  <xsl:template match="ATTEMPTFILE">
    <xsl:variable name="attemptid" select="ATTEMPTID/@value"/>
    <xsl:variable name="relurl" select="concat( ../../../@xml:base, '/', @id, '/', FILE/NAME/text() )"/>
    <li>
      <a href="{$relurl}"><xsl:value-of select="FILE/NAME/text()"/></a>
    </li>
  </xsl:template>
  
  <xsl:template match="STUDENTSUBMISSION">
    <xsl:variable name="attemptid" select="../@id"/>
    <xsl:variable name="embeddedresources" select="/bbcarv:student/bbcarv:resources/bbcarv:resource[@type = 'course/x-bb-attemptembeddedfiles' and count(//ATTEMPTID[@value = $attemptid]) &gt; 0]"/>
    <xsl:variable name="embeddedbase">
      <xsl:choose>
        <xsl:when test="count($embeddedresources) = 1">
          <xsl:value-of select="$embeddedresources[1]/@xml:base"/>
          <xsl:text>/</xsl:text>
          <xsl:value-of select="$attemptid"/>
          <xsl:text>/embedded/</xsl:text>
        </xsl:when>
        <xsl:otherwise>unknownpath/</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
 
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.location@X@'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="INSTRUCTORCOMMENTS">
    <xsl:variable name="attemptid" select="../@id"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/attempt/', $attemptid, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>
    
  <xsl:template match="INSTRUCTORNOTES">
    <xsl:variable name="attemptid" select="../@id"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/attemptNotes/', $attemptid, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="textoutput">
    <xsl:param name="t"/>
    <xsl:param name="base"/>
    <xsl:param name="v"/>
    <xsl:variable name="original" select="TEXT/text()"/>
    <xsl:variable name="rebased" select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.replace( $original, $v, $base )"/>
    
    <xsl:choose>
      <xsl:when test="$t/TYPE/@value = 'H'">
        <div class="userhtml">
          <xsl:value-of select="$rebased" disable-output-escaping="yes"/>
        </div>
      </xsl:when>
      <xsl:otherwise>
        <div class="userplaintext">
          <xsl:value-of select="$original"/>        
        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
