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
  exclude-result-prefixes="bb"
  version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:variable name="u">
    <usersfile>
      <xsl:if test="count(/manifest/resources/resource[@type = 'course/x-bb-user']) = 1">
        <xsl:copy-of select="document( /manifest/resources/resource[@type = 'course/x-bb-user']/@bb:file )/USERS/USER"/>
      </xsl:if>
    </usersfile>
  </xsl:variable>
    
  <xsl:template match="/">
    <xsl:text>
</xsl:text>
    <students>
      <!--
      <debug>
        <xsl:copy-of select="nodeset( $u )"/>
      </debug>
            -->
      <xsl:for-each select="nodeset( $u )/usersfile/USER[string-length(STUDENTID/@value) = 8]">
        <item>
          <xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
          <xsl:copy-of select="STUDENTID"/>
          <xsl:copy-of select="NAMES"/>
          <xsl:copy-of select="INSTITUTIONEMAIL"/>
        </item>
      </xsl:for-each>
    </students>
  </xsl:template>

</xsl:stylesheet>
