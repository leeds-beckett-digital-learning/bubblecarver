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

  <xsl:variable name="gradebook" select="bbcarv:student/bbcarv:resources/bbcarv:resource[@type='course/x-bb-gradebook']/bbcarv:data/GRADEBOOK"/>
  
  <xsl:template match="/">
    <html>
      <head>
        <title>Student Course Report <xsl:value-of select="bbcarv:student/@sid"/></title>
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
              <th>Type</th>
              <th>Points Avail.</th>
              <th>Points</th>
              <th>Grade</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="$gradebook/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION"/>
          </tbody>
        </table>
        <h1>Grade Centre History</h1>
        <xsl:apply-templates select="$gradebook/GRADE_HISTORY_ENTRIES/GRADE_HISTORY_ENTRY"/>
        <script src="../../../../../../script/report.js"></script>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="OUTCOMEDEFINITION">
    <xsl:variable name="title">
      <xsl:choose>
        <xsl:when test="TITLE/@value = 'OutcomeDefinition.Total.title'">Total</xsl:when>
        <xsl:when test="TITLE/@value = 'OutcomeDefinition.WeightedTotal.title'">Weighted Total</xsl:when>
        <xsl:otherwise><xsl:value-of select="TITLE/@value"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <tr>
      <th><xsl:value-of select="$title"/></th>
      <td>
        <xsl:if test="count( GROUPATTEMPTS/GROUPATTEMPT ) &gt; 0">Group </xsl:if>
        <xsl:choose>
          <xsl:when test="'true' = ISCALCULATED/@value">Calculated: <xsl:value-of select="CALCULATIONTYPE/@value"/></xsl:when>
          <xsl:when test="string-length( LTI_DOMAIN_ID/@value ) &gt; 0">LTI Tool</xsl:when>
          <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-assignment'">BBL Assignment</xsl:when>
          <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-bloglink'">Blog or Journal</xsl:when>
          <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-wiki'">Wiki</xsl:when>
          <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-plugin-scormengine'">SCORM Package</xsl:when>
          <xsl:otherwise>Other</xsl:otherwise>
        </xsl:choose>
      </td>
      <td><xsl:value-of select="POINTSPOSSIBLE/@value"/></td>
      <td>
        <xsl:apply-templates select="OUTCOMES/OUTCOME" mode="points"/>        
      </td>
      <td>
        <xsl:apply-templates select="OUTCOMES/OUTCOME" mode="grade"/>        
      </td>
    </tr>
    <xsl:if test="count( OUTCOMES/OUTCOME ) = 1">
      <tr>
        <th>
          <span style="display: none;" class="collapse-detail" id="{concat( 'collapser-', @id )}" data-id="{concat( @id )}">
            <svg width="20px" height="20px" fill="#000000" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg"><g id="SVGRepo_bgCarrier" stroke-width="0"></g><g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g><g id="SVGRepo_iconCarrier"><title>box-minus</title><path d="M96 448Q82 448 73 439 64 430 64 416L64 96Q64 82 73 73 82 64 96 64L416 64Q430 64 439 73 448 82 448 96L448 416Q448 430 439 439 430 448 416 448L96 448ZM368 280L368 232 144 232 144 280 368 280Z"></path></g></svg>
          </span>
          <span class="expand-detail" id="{concat( 'expander-', @id )}" data-id="{concat( @id )}">
            <svg width="20px" height="20px" fill="#000000" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg"><g id="SVGRepo_bgCarrier" stroke-width="0"></g><g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g><g id="SVGRepo_iconCarrier"><title>box-plus</title><path d="M96 448Q82 448 73 439 64 430 64 416L64 96Q64 82 73 73 82 64 96 64L416 64Q430 64 439 73 448 82 448 96L448 416Q448 430 439 439 430 448 416 448L96 448ZM280 368L280 280 368 280 368 232 280 232 280 144 232 144 232 232 144 232 144 280 232 280 232 368 280 368Z"></path></g></svg>
          </span>
        </th>
        <td colspan="4">
          <div class="detail" style="display: none" id="{concat( 'detail-', @id )}">
          <h2>Detail</h2>
          <xsl:apply-templates select="OUTCOMES/OUTCOME"/>
          </div>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="OUTCOME" mode="points">
    <xsl:choose>
      <xsl:when test="count( OVERRIDE_SCORE ) = 0">
        See attempt: <xsl:value-of select="../../AGGREGATIONMODEL/@value"/>        
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="OVERRIDE_SCORE/text()"/> (Attempt override)
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="OUTCOME" mode="grade">
    <xsl:choose>
      <xsl:when test="count( OVERRIDE_GRADE ) = 0">
        See attempt: <xsl:value-of select="../../AGGREGATIONMODEL/@value"/>        
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="OVERRIDE_GRADE/text()"/> (Attempt override)
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="OUTCOME">
    <xsl:apply-templates select="ATTEMPTS/ATTEMPT"/>
  </xsl:template>
  
  <xsl:template match="ATTEMPT">
    <xsl:variable name="attemptid" select="@id"/>
    <h2>Attempt <xsl:value-of select="@id"/> <xsl:value-of select="DATEATTEMPTED/@value"/></h2>
    <table>
      <tbody>
        <tr>
          <th>Status:</th>
          <td><xsl:value-of select="STATUS/@value"/></td>
        </tr>
        <tr>
          <th>First Graded:</th>
          <td><xsl:value-of select="DATEFIRSTGRADEDED/@value"/></td>
        </tr>
        <tr>
          <th>Last Graded:</th>
          <td><xsl:value-of select="DATELASTGRADED/@value"/></td>
        </tr>
        <tr>
          <th>Score:</th>
          <td><xsl:value-of select="SCORE/@value"/></td>
        </tr>
        <tr>
          <th>Grade:</th>
          <td><xsl:value-of select="GRADE/@value"/></td>
        </tr>
      </tbody>
    </table>
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
      <h3>Feedback</h3>
      <xsl:apply-templates select="INSTRUCTORCOMMENTS"/>
    </div>
    <div class="attempt-notes">
      <h3>Marker Private Notes</h3>
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

  <xsl:template match="GRADE_HISTORY_ENTRY">
    <xsl:variable name="attemptid" select="ATTEMPT_ID/@value"/>
    <xsl:variable name="epoch" select="DATE_LOGGED/@bbcarv:epoch"/>
    <div>
      <h2><xsl:value-of select="DATE_LOGGED/@value"/></h2>
      <table>
        <tr>
          <th>Action:</th>
          <td><xsl:value-of select="ENTRY_KEY/@value"/></td>
        </tr>
      </table>
      <div class="attempt-comments">
        <xsl:apply-templates select="FEEDBACK_TO_USER"/>
      </div>      
      <!-- In grade history private notes are marked up similar to comments in attempt  -->
      <div class="attempt-notes">
        <xsl:apply-templates select="INSTRUCTOR_COMMENTS"/>
      </div>      
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
    
  <xsl:template match="FEEDBACK_TO_USER">
    <xsl:variable name="attemptid" select="../ATTEMPT_ID/@value"/>
    <xsl:variable name="attempt" select="$gradebook/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT[@id = $attemptid]"/>
    <xsl:variable name="outcomeid" select="$attempt/../../@id"/>
    <xsl:variable name="epoch" select="../DATE_LOGGED/@bbcarv:epoch"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/gradeHistory/', $outcomeid, '/', $epoch, '/' )"/>
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

  <!-- Grade history - BUT USED FOR PRIVATE NOTES!!!!!  -->
  <xsl:template match="INSTRUCTOR_COMMENTS">
    <xsl:variable name="attemptid" select="../ATTEMPT_ID/@value"/>
    <xsl:variable name="attempt" select="$gradebook/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT[@id = $attemptid]"/>
    <xsl:variable name="outcomeid" select="$attempt/../../@id"/>
    <xsl:variable name="epoch" select="../DATE_LOGGED/@bbcarv:epoch"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/gradeHistoryNotes/', $outcomeid, '/', $epoch, '/' )"/>
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
