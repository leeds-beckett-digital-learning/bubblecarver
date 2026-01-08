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
      </head>
      <body>
        <h1><xsl:value-of select="bbcarv:student/@sid"/></h1>
        <h2>
          <xsl:value-of select=".//USERS/USER/NAMES/FAMILY/@value"/>,
          <xsl:value-of select=".//USERS/USER/NAMES/GIVEN/@value"/> 
        </h2>
        <h3>Grade Centre Data</h3>
        <xsl:apply-templates select="bbcarv:student/bbcarv:resources/bbcarv:resource/bbcarv:data/GRADEBOOK/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION"/>
        <!--
        <hr/>
        <xsl:for-each select="//TEXT">
          <div>
            <xsl:value-of select="text()" disable-output-escaping="yes"/>
          </div>
        </xsl:for-each>
                -->
      </body>
    </html>
  </xsl:template>

  <xsl:template match="OUTCOMEDEFINITION">
    <h4><xsl:value-of select="TITLE/@value"/></h4>
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
    <h5>Attempt <xsl:value-of select="@id"/></h5>
    <div>
      <xsl:apply-templates select="STUDENTSUBMISSION"/>
    </div>
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
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template match="INSTRUCTORCOMMENTS | INSTRUCTORNOTES">
  </xsl:template>
  
  <xsl:template name="textoutput">
    <xsl:param name="t"/>
    <xsl:param name="base"/>
    <xsl:variable name="original" select="TEXT/text()"/>
    <xsl:variable name="rebased" select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.replace( $original, '@X@EmbeddedFile.location@X@', $base )"/>
    
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
