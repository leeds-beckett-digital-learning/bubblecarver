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
  version="1.0">
  <xsl:output method="xml"/>

  
  
  <xsl:template match="/">
    <students>
      <debug><xsl:value-of select="/manifest/resources/resource[@type = 'course/x-bb-user']/@bb:file"/></debug>
    </students>
  </xsl:template>

</xsl:stylesheet>
