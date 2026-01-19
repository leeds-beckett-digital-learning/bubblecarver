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
  xmlns:bbcarv="http://www.leedsbeckett.ac.uk/bubblecarver"
  xmlns:cat="http://www.leedsbeckett.ac.uk/bubblecarver/catalogue"
  xmlns:java="http://xml.apache.org/xalan/java"
  extension-element-prefixes="redirect java"
  exclude-result-prefixes="bb"
  version="1.0">
  <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

  <xsl:param name="filename"/>

  <xsl:template match="/">
    <xsl:text>
</xsl:text>
    <archive>
      <xsl:attribute name="bbcarv:archivename"><xsl:value-of select="$filename"/></xsl:attribute>
      <xsl:for-each select="/manifest/resources/resource">
        <xsl:copy>
          <xsl:attribute name="bbcarv:archivename"><xsl:value-of select="$filename"/></xsl:attribute>
          <xsl:for-each select="@*">
            <xsl:copy-of select="."/>
          </xsl:for-each>
        </xsl:copy>
      </xsl:for-each>
      
    </archive>
  </xsl:template>
  
</xsl:stylesheet>
