<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : liststudents.xsl
    Created on : 16 December 2025, 12:09
    Author     : maber01
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:bb="http://www.blackboard.com/content-packaging/"
  xmlns:bbcarv="http://www.leedsbeckett.ac.uk/bubblecarver"
  exclude-result-prefixes="bb"
  version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:key name="bytype" match="resource" use="@type" />  
  
  <xsl:template match="/">
    <xsl:text>
</xsl:text>

    <summary>
      <xsl:for-each select="/digest/archive/resource[ count( . | key( 'bytype', @type )[1] ) = 1]">
        <xsl:sort select="@type"/>
        <xsl:variable name="t" select="@type"/>
        <xsl:variable name="n" select="count(/digest/archive/resource[@type = $t])"/>
        <type type="{@type}" count="{$n}">
          <xsl:for-each select="/digest/archive/resource[ @type = $t ]">
            <xsl:if test="position() &lt; 101">
              <resource archivename="{@bbcarv:archivename}"/>
            </xsl:if>
          </xsl:for-each>
        </type>
      </xsl:for-each>
    </summary>
    
  </xsl:template>
  
</xsl:stylesheet>
