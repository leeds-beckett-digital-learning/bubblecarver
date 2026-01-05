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
  xmlns:redirect="http://xml.apache.org/xalan/redirect"
  extension-element-prefixes="redirect"
  exclude-result-prefixes="bb"
  version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="output-base-uri" select="'unknown path to nowhere'"/>
  
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
        <xsl:variable name="sid" select="STUDENTID/@value"/>
        <xsl:variable name="student-file">
          <xsl:call-template name="make-student-uri">
            <xsl:with-param name="sid" select="$sid"/>
          </xsl:call-template>
        </xsl:variable>
        <student>
          <xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
          <xsl:attribute name="sid"><xsl:value-of select="$sid"/></xsl:attribute>
          <xsl:attribute name="student-file"><xsl:value-of select="$student-file"/></xsl:attribute>
          <xsl:copy-of select="NAMES"/>
          <xsl:copy-of select="INSTITUTIONEMAIL"/>
        </student>
        
        <redirect:write file="{$student-file}">
          <student sid="{$sid}">
          </student>
        </redirect:write>        
        
      </xsl:for-each>
    </students>
  </xsl:template>

  <xsl:template name="make-student-uri">
    <xsl:param name="sid"/>
    <xsl:value-of select="$output-base-uri" />
    <xsl:text>\</xsl:text>
    <xsl:value-of select="substring($sid,0,2)" />
    <xsl:text>______\</xsl:text>
    <xsl:value-of select="substring($sid,0,4)" />
    <xsl:text>____\</xsl:text>
    <xsl:value-of select="substring($sid,0,6)" />
    <xsl:text>__\</xsl:text>
    <xsl:value-of select="$sid" />
    <xsl:text>\</xsl:text>
    <xsl:value-of select="$sid" />
    <xsl:text>.xml</xsl:text>
  </xsl:template>
  
</xsl:stylesheet>
